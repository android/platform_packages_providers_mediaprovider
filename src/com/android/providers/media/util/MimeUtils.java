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

package com.android.providers.media.util;

import android.content.ClipDescription;
import android.mtp.MtpConstants;
import android.provider.MediaStore.Files.FileColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class MimeUtils {
    public static final Set<String> sPlaylistMimes = new HashSet<String>();
    public static final Set<String> sDocumentMimes = new HashSet<String>();
    public static final Set<String> sSubtitleMimes = new HashSet<String>();

    static {
        sPlaylistMimes.add("application/vnd.apple.mpegurl");
        sPlaylistMimes.add("application/vnd.ms-wpl");
        sPlaylistMimes.add("application/x-extension-smpl");
        sPlaylistMimes.add("application/x-mpegurl");
        sPlaylistMimes.add("application/xspf+xml");
        sPlaylistMimes.add("audio/mpegurl");
        sPlaylistMimes.add("audio/x-mpegurl");
        sPlaylistMimes.add("audio/x-scpls");

        sSubtitleMimes.add("application/lrc");
        sSubtitleMimes.add("application/smil+xml");
        sSubtitleMimes.add("application/ttml+xml");
        sSubtitleMimes.add("application/x-extension-cap");
        sSubtitleMimes.add("application/x-extension-srt");
        sSubtitleMimes.add("application/x-extension-sub");
        sSubtitleMimes.add("application/x-extension-vtt");
        sSubtitleMimes.add("application/x-subrip");
        sSubtitleMimes.add("text/vtt");

        sDocumentMimes.add("application/epub+zip");
        sDocumentMimes.add("application/msword");
        sDocumentMimes.add("application/pdf");
        sDocumentMimes.add("application/rtf");
        sDocumentMimes.add("application/vnd.ms-excel");
        sDocumentMimes.add("application/vnd.ms-excel.addin.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-excel.sheet.binary.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-excel.sheet.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-excel.template.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-powerpoint");
        sDocumentMimes.add("application/vnd.ms-powerpoint.addin.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-powerpoint.presentation.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-powerpoint.slideshow.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-powerpoint.template.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-word.document.macroenabled.12");
        sDocumentMimes.add("application/vnd.ms-word.template.macroenabled.12");
        sDocumentMimes.add("application/vnd.oasis.opendocument.chart");
        sDocumentMimes.add("application/vnd.oasis.opendocument.database");
        sDocumentMimes.add("application/vnd.oasis.opendocument.formula");
        sDocumentMimes.add("application/vnd.oasis.opendocument.graphics");
        sDocumentMimes.add("application/vnd.oasis.opendocument.graphics-template");
        sDocumentMimes.add("application/vnd.oasis.opendocument.presentation");
        sDocumentMimes.add("application/vnd.oasis.opendocument.presentation-template");
        sDocumentMimes.add("application/vnd.oasis.opendocument.spreadsheet");
        sDocumentMimes.add("application/vnd.oasis.opendocument.spreadsheet-template");
        sDocumentMimes.add("application/vnd.oasis.opendocument.text");
        sDocumentMimes.add("application/vnd.oasis.opendocument.text-master");
        sDocumentMimes.add("application/vnd.oasis.opendocument.text-template");
        sDocumentMimes.add("application/vnd.oasis.opendocument.text-web");
        sDocumentMimes.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        sDocumentMimes.add("application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        sDocumentMimes.add("application/vnd.openxmlformats-officedocument.presentationml.template");
        sDocumentMimes.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        sDocumentMimes.add("application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        sDocumentMimes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        sDocumentMimes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        sDocumentMimes.add("application/vnd.stardivision.calc");
        sDocumentMimes.add("application/vnd.stardivision.chart");
        sDocumentMimes.add("application/vnd.stardivision.draw");
        sDocumentMimes.add("application/vnd.stardivision.impress");
        sDocumentMimes.add("application/vnd.stardivision.impress-packed");
        sDocumentMimes.add("application/vnd.stardivision.mail");
        sDocumentMimes.add("application/vnd.stardivision.math");
        sDocumentMimes.add("application/vnd.stardivision.writer");
        sDocumentMimes.add("application/vnd.stardivision.writer-global");
        sDocumentMimes.add("application/vnd.sun.xml.calc");
        sDocumentMimes.add("application/vnd.sun.xml.calc.template");
        sDocumentMimes.add("application/vnd.sun.xml.draw");
        sDocumentMimes.add("application/vnd.sun.xml.draw.template");
        sDocumentMimes.add("application/vnd.sun.xml.impress");
        sDocumentMimes.add("application/vnd.sun.xml.impress.template");
        sDocumentMimes.add("application/vnd.sun.xml.math");
        sDocumentMimes.add("application/vnd.sun.xml.writer");
        sDocumentMimes.add("application/vnd.sun.xml.writer.global");
        sDocumentMimes.add("application/vnd.sun.xml.writer.template");
        sDocumentMimes.add("application/x-mspublisher");
    }

    /**
     * Variant of {@link Objects#equal(Object, Object)} but which tests with
     * case-insensitivity.
     */
    public static boolean equalIgnoreCase(@Nullable String a, @Nullable String b) {
        return (a != null) && a.equalsIgnoreCase(b);
    }

    /**
     * Variant of {@link String#startsWith(String)} but which tests with
     * case-insensitivity.
     */
    public static boolean startsWithIgnoreCase(@Nullable String target, @Nullable String other) {
        if (target == null || other == null) return false;
        if (other.length() > target.length()) return false;
        return target.regionMatches(true, 0, other, 0, other.length());
    }

    /**
     * Resolve the MIME type of the given file, returning
     * {@code application/octet-stream} if the type cannot be determined.
     */
    public static @NonNull String resolveMimeType(@NonNull File file) {
        final String extension = FileUtils.extractFileExtension(file.getPath());
        if (extension == null) return ClipDescription.MIMETYPE_UNKNOWN;

        final String mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
        if (mimeType == null) return ClipDescription.MIMETYPE_UNKNOWN;

        return mimeType;
    }

    /**
     * Resolve the {@link FileColumns#MEDIA_TYPE} of the given MIME type. This
     * carefully checks for more specific types before generic ones, such as
     * treating {@code audio/mpegurl} as a playlist instead of an audio file.
     */
    public static int resolveMediaType(@NonNull String mimeType) {
        if (isPlaylistMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_PLAYLIST;
        } else if (isSubtitleMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_SUBTITLE;
        } else if (isAudioMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_AUDIO;
        } else if (isVideoMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_VIDEO;
        } else if (isImageMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_IMAGE;
        } else if (isDocumentMimeType(mimeType)) {
            return FileColumns.MEDIA_TYPE_DOCUMENT;
        } else {
            return FileColumns.MEDIA_TYPE_NONE;
        }
    }

    /**
     * Resolve the {@link FileColumns#FORMAT} of the given MIME type. Note that
     * since this column isn't public API, we're okay only getting very rough
     * values in place, and it's not worthwhile to build out complex matching.
     */
    public static int resolveFormatCode(@Nullable String mimeType) {
        final int mediaType = resolveMediaType(mimeType);
        switch (mediaType) {
            case FileColumns.MEDIA_TYPE_AUDIO:
                return MtpConstants.FORMAT_UNDEFINED_AUDIO;
            case FileColumns.MEDIA_TYPE_VIDEO:
                return MtpConstants.FORMAT_UNDEFINED_VIDEO;
            case FileColumns.MEDIA_TYPE_IMAGE:
                return MtpConstants.FORMAT_DEFINED;
            default:
                return MtpConstants.FORMAT_UNDEFINED;
        }
    }

    public static @NonNull String extractPrimaryType(@NonNull String mimeType) {
        final int slash = mimeType.indexOf('/');
        if (slash == -1) {
            throw new IllegalArgumentException();
        }
        return mimeType.substring(0, slash);
    }

    public static boolean isAudioMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        return startsWithIgnoreCase(mimeType, "audio/");
    }

    public static boolean isVideoMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        return startsWithIgnoreCase(mimeType, "video/");
    }

    public static boolean isImageMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;
        return startsWithIgnoreCase(mimeType, "image/");
    }

    public static boolean isPlaylistMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;

        return sPlaylistMimes.contains(mimeType.toLowerCase(Locale.ROOT));
    }

    public static boolean isSubtitleMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;

        return sSubtitleMimes.contains(mimeType.toLowerCase(Locale.ROOT));
    }

    public static boolean isDocumentMimeType(@Nullable String mimeType) {
        if (mimeType == null) return false;

        if (startsWithIgnoreCase(mimeType, "text/")) return true;

        return sDocumentMimes.contains(mimeType.toLowerCase(Locale.ROOT));
    }
}
