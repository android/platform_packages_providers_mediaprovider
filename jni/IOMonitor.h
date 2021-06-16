/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef MEDIAPROVIDER_JNI_IOMONITOR_H_
#define MEDIAPROVIDER_JNI_IOMONITOR_H_

#include <android-base/logging.h>
#include <sys/time.h>
#include <sys/types.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <cstdint>
#include <ctime>
#include <list>
#include <map>
#include <memory>
#include <mutex>
#include <queue>
#include <shared_mutex>
#include <sstream>
#include <string>
#include <thread>
#include <unordered_map>
#include <utility>
#include <vector>

namespace mediaprovider {
namespace fuse {

using namespace std::chrono_literals;
constexpr auto kCollectDuration = 3min;
constexpr auto kCollectCount = 5;
constexpr auto kCollectUidCount = 10;

// We only monitor some important IOs to application behaviors
enum FuseIOCode {
    kFuseIOGetattr,
    kFuseIOUnlink,
    kFuseIOOpen,
    kFuseIORead,
    kFuseIOWrite,
    kFuseIOReaddir,
    kFuseIOAccess,
    kFuseIOCreate,
    kFuseIOMax
};

const std::map<FuseIOCode, std::string> kIOCodeMap = {
        {kFuseIOGetattr, "GETATTR"}, {kFuseIOUnlink, "UNLINK"}, {kFuseIOOpen, "OPEN"},
        {kFuseIORead, "READ"},       {kFuseIOWrite, "WRITE"},   {kFuseIOReaddir, "READDIR"},
        {kFuseIOAccess, "ACCESS"},   {kFuseIOCreate, "CREATE"}};

struct PerUidIOCount {
    PerUidIOCount() : total(0), count{0} {}

    void Merge(const struct PerUidIOCount& rhs) {
        total += rhs.total;
        for (int i = 0; i < kFuseIOMax; i++) {
            count[i] += rhs.count[i];
        }
    }

    uint32_t total;
    std::array<uint32_t, kFuseIOMax> count;
};

using uid_map_t = std::unordered_map<uid_t, PerUidIOCount>;
class IOMonitor;

class PerThreadIO {
  public:
    PerThreadIO()
        : uid_map_(std::make_unique<uid_map_t>()),
          io_mon_(nullptr),
          registerPerThreadIO_(nullptr),
          unregisterPerThreadIO_(nullptr),
          mergePerThreadIO_(nullptr) {}

    ~PerThreadIO() {
        if (io_mon_ == nullptr) return;
        if (unregisterPerThreadIO_ != nullptr) (io_mon_->*unregisterPerThreadIO_)(this);
        if (mergePerThreadIO_ != nullptr) (io_mon_->*mergePerThreadIO_)(std::move(uid_map_));
    }

    std::unique_ptr<uid_map_t> GetPerThreadIO() {
        std::lock_guard<std::mutex> guard(lock_);
        std::unique_ptr<uid_map_t> ptr = std::move(uid_map_);
        uid_map_ = std::make_unique<uid_map_t>();
        return ptr;
    }

    void Insert(uint32_t io_code, uid_t uid, IOMonitor* io_mon) {
        if (__builtin_expect(this->io_mon_ == nullptr, 0)) {
            SetCallback();
            this->io_mon_ = io_mon;
            (io_mon_->*registerPerThreadIO_)(this);
        }

        std::lock_guard<std::mutex> guard(lock_);
        auto& per_uid = (*uid_map_)[uid];
        per_uid.total++;
        per_uid.count[io_code]++;
    }

  private:
    using func1 = void (IOMonitor::*)(PerThreadIO*);
    using func2 = void (IOMonitor::*)(std::unique_ptr<uid_map_t>);

    void SetCallback();

    std::unique_ptr<uid_map_t> uid_map_;
    mutable std::mutex lock_;
    IOMonitor* io_mon_;
    func1 registerPerThreadIO_;
    func1 unregisterPerThreadIO_;
    func2 mergePerThreadIO_;
};

using uid_pair_t = std::pair<uid_t, PerUidIOCount>;
struct IOStat {
    std::string GetSummary() {
        std::stringstream ss;
        ss << "(" << FormatTime(start_t) << " to " + FormatTime(end_t) + "):" << std::endl;
        ss << "  UID   TOTAL ";
        for (auto opiter = kIOCodeMap.begin(); opiter != kIOCodeMap.end(); opiter++)
            ss << " " << opiter->second << " ";

        ss << std::endl;
        for (auto iter = uid_io.begin(); iter != uid_io.end(); iter++) {
            ss << StringFormat("%7d%7u", iter->first, iter->second.total);
            for (uint32_t count : iter->second.count) {
                ss << StringFormat("%7u", count);
            }
            ss << std::endl;
        }
        ss << std::endl;
        return ss.str();
    }

    static std::string FormatTime(struct timeval tv) {
        struct tm* t = localtime(&tv.tv_sec);
        if (t == nullptr) {
            return "NULL";
        }
        return StringFormat("%04d-%02d-%02d %02d:%02d:%02d.%03ld", t->tm_year + 1900, t->tm_mon + 1,
                            t->tm_mday, t->tm_hour, t->tm_min, t->tm_sec, tv.tv_usec);
    }

    template <typename... Args>
    static std::string StringFormat(const std::string& format, Args... args) {
        size_t size = snprintf(nullptr, 0, format.c_str(), args...) + 1;
        std::unique_ptr<char[]> buffer(new char[size]);
        snprintf(buffer.get(), size, format.c_str(), args...);
        return std::string(buffer.get(), buffer.get() + size - 1);
    }

    std::vector<uid_pair_t> uid_io;
    struct timeval start_t;
    struct timeval end_t;
};

class IOStatQueue {
  public:
    IOStatQueue() : max_count_(kCollectCount) {}
    ~IOStatQueue() {}

    void Push(IOStat* io_stat) {
        std::lock_guard<std::mutex> guard(lock_);
        if (q_.size() >= max_count_) {
            q_.pop();
        }
        q_.push(std::move(*io_stat));
    }

    bool Pop(IOStat* io_stat) {
        std::lock_guard<std::mutex> guard(lock_);
        if (q_.empty()) return false;

        *io_stat = std::move(q_.front());
        q_.pop();
        return true;
    }

  private:
    std::mutex lock_;
    std::queue<IOStat> q_;
    int max_count_;
};

class IOMonitor {
  public:
    IOMonitor() : should_stop_(false), collect_thread_(&IOMonitor::CollectIO, this) {}

    ~IOMonitor() {
        should_stop_ = true;
        collect_thread_.join();
    }

    void MergePerThreadIO(std::unique_ptr<uid_map_t> ptr) {
        std::lock_guard<std::mutex> guard(lock_);
        for (auto& uid_map : *ptr) {
            collect_io_[uid_map.first].Merge(uid_map.second);
        }
    }

    void RegisterPerThreadIO(PerThreadIO* pts) {
        std::lock_guard<std::mutex> guard(lock_);
        thread_list_.push_back(pts);
    }

    void UnregisterPerThreadIO(PerThreadIO* pts) {
        std::lock_guard<std::mutex> guard(lock_);
        for (auto iter = thread_list_.cbegin(); iter != thread_list_.cend(); iter++) {
            if (*iter == pts) {
                thread_list_.erase(iter);
                return;
            }
        }
    }

    std::string GetIOStats() {
        std::stringstream ss;

        IOStat io_stat;
        while (queue_.Pop(&io_stat)) {
            ss << io_stat.GetSummary();
        }
        return ss.str();
    }

  private:
    void CollectIO() {
        while (!should_stop_) {
            struct timeval start_t;
            gettimeofday(&start_t, 0);
            std::this_thread::sleep_for(kCollectDuration);

            struct timeval end_t;
            gettimeofday(&end_t, 0);
            CollectIOFromFuseThreads();

            std::vector<uid_pair_t> uid_io = GetAndResetCollectIO();
            if (!uid_io.empty()) {
                sort(uid_io.begin(), uid_io.end(), CompareTotal);
                if (uid_io.size() > kCollectUidCount) {
                    uid_io.resize(kCollectUidCount);
                }
                IOStat io_stat = {uid_io, start_t, end_t};
                queue_.Push(&io_stat);
            }
        }
    }

    void CollectIOFromFuseThreads() {
        std::lock_guard<std::mutex> lock(lock_);
        for (auto iter = thread_list_.cbegin(); iter != thread_list_.cend(); iter++) {
            std::unique_ptr<uid_map_t> ptr = (*iter)->GetPerThreadIO();

            for (auto& uid_map : *ptr) {
                collect_io_[uid_map.first].Merge(uid_map.second);
            }
        }
    }

    static bool CompareTotal(const uid_pair_t& a, const uid_pair_t& b) {
        return (a.second.total > b.second.total);
    }

    std::vector<uid_pair_t> GetAndResetCollectIO() {
        std::lock_guard<std::mutex> lock(lock_);
        std::vector<uid_pair_t> uid_io(collect_io_.begin(), collect_io_.end());
        collect_io_.clear();
        return uid_io;
    }

    std::mutex lock_;
    std::list<PerThreadIO*> thread_list_;
    bool should_stop_;
    uid_map_t collect_io_;
    IOStatQueue queue_;
    std::thread collect_thread_;
};

void PerThreadIO::SetCallback(void) {
    registerPerThreadIO_ = &IOMonitor::RegisterPerThreadIO;
    unregisterPerThreadIO_ = &IOMonitor::UnregisterPerThreadIO;
    mergePerThreadIO_ = &IOMonitor::MergePerThreadIO;
}

thread_local PerThreadIO per_thread_io;

}  // namespace fuse
}  // namespace mediaprovider

#endif  // MEDIAPROVIDER_JNI_IOMONITOR_H_
