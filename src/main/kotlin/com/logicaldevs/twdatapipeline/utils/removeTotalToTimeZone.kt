package com.logicaldevs.twdatapipeline.utils

fun removeByString(input: String,splitBy:String): String {
    val string = try {
        input.split(" $splitBy ")[0]
    } catch (ex: Exception) {
        input
    }
    println("HAMZA MUAZZAM $string")
    return string
}

// Example usage
fun main() {
    val input = "Furnace Oil Installation Caltex Rd New Lalazar Rawalpindi Punjab 0 0 0.00 00:00:00 00:01:31 33.5517 73.0689 Total 0.00 0d, 00:00:00 0d, 19:18:42 Activity Report (AA112) Reg No: AA112 From : Tuesday, July 25, 2023 To: Saturday, July 29, 2023 Time Zone: GMT +05:00 Group Name RDT LandMark Speed Directio n Distance Travel"
    val result = removeByString(input,"Total")
}
