package com.tans.tfiletransfer.net.transferproto.filetransfer

import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import okio.FileSystem
import okio.Path
import okio.SYSTEM

val fileSystem by lazy {
    FileSystem.SYSTEM
}

fun calculateFileSegmentRanges(
    file: ExplorerFile,
    minFileSegmentSize: Long,
    maxSegmentCount: Int
): List<Pair<Long, Long>> {
    val fileSize = file.size
    if (fileSize <= 0L) return emptyList()

    val minSeg = if (minFileSegmentSize <= 0L) fileSize else minFileSegmentSize
    val maxSegs = maxSegmentCount.coerceAtLeast(1)

    val segCount = when {
        fileSize < minSeg -> 1
        else -> (fileSize / minSeg).coerceAtMost(maxSegs.toLong()).toInt()
    }

    val baseSize = fileSize / segCount
    val remainder = (fileSize % segCount).toInt()

    var start = 0L
    val ranges = ArrayList<Pair<Long, Long>>(segCount)
    for (i in 0 until segCount) {
        val sizeI = baseSize + if (i < remainder) 1L else 0L
        val endExclusive = start + sizeI
        ranges.add(start to endExclusive)
        start = endExclusive
    }
    return ranges
}

fun resolveUniqueLocalFilePath(
    dirPath: Path,
    expectName: String,
    createRealFile: Boolean
): Path {
    if (!fileSystem.exists(dirPath)) {
        fileSystem.createDirectories(dirPath)
    }
    val dotIndex = expectName.lastIndexOf('.')
    val hasExt = dotIndex > 0 && dotIndex < expectName.length - 1
    val stem = if (hasExt) expectName.take(dotIndex) else expectName
    val ext = if (hasExt) expectName.substring(dotIndex + 1) else null
    var index = 0
    while (true) {
        val candidate =
            if (index == 0) expectName else if (ext != null) "$stem-$index.$ext" else "$stem-$index"
        val p = dirPath / candidate
        if (!fileSystem.exists(p)) {
            if (createRealFile) {
                fileSystem.write(p) { }
            }
            return p
        }
        index++
    }
}

fun Long.toHumanReadableSize(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = if (this < 0L) 0.0 else this.toDouble()
    var unitIndex = 0
    while (size >= 1024.0 && unitIndex < units.size - 1) {
        size /= 1024.0
        unitIndex++
    }
    val scaled = kotlin.math.round(size * 100.0).toLong()
    val intPart = (scaled / 100).toString()
    val fracPart = (scaled % 100).toString().padStart(2, '0')
    return "$intPart.$fracPart ${units[unitIndex]}"
}

fun Long.toHumanReadableSpeed(): String {
    return toHumanReadableSize() + "/s"
}
