package com.study.reminder.helper

/**
 * 学习时段判定缓存。
 * 时间段 / 课表数据变化时必须调用 invalidate()，
 * 避免无障碍服务继续使用过期的“在学习时段内”结果。
 */
object StudyTimeCache {
    @Volatile var cachedInStudyTime: Boolean? = null
    @Volatile var cacheTime: Long = 0L

    fun invalidate() {
        cachedInStudyTime = null
        cacheTime = 0L
    }
}
