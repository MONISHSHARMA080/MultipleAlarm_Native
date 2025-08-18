package com.example.trying_native

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Verifies that the given [condition] is true, otherwise throws an exception.
 *
 * @param condition the boolean expression that must hold
 * @param message   optional detail message to include in the exception
 * @throws IllegalStateException if [condition] evaluates to false
 */
fun assertWithException(condition:Boolean, message:String){
    if (!condition){
        throw  IllegalStateException(message)
    }
}

fun incrementTheStartCalenderTimeUntilItIsInFuture(startCalendar: Calendar, currentCalendar:Calendar):Calendar{
    // cause we will start the comparison from the date today else the hard limit will not work
    startCalendar.set(Calendar.DATE, currentCalendar.get(Calendar.DATE) )

    for (i in 0..1000){
//        logD("the loop iteration in the incrementStartCalender is ->$i")
        if (startCalendar.timeInMillis >= currentCalendar.timeInMillis){
            break
        }else{
            startCalendar.set(Calendar.DATE, currentCalendar.get(Calendar.DATE) + 1 )
        }
    }
    return startCalendar
}

fun getDateForDisplay(calendar: Calendar):String{
    return  calendar.time.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}