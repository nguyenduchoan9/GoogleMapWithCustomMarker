package solution.com.googlemap

data class POI(val type: String,
               val name: String,
               val latitude: String,
               val longitude: String) {
    fun getLangtitudeInDouble(): Double{
        return latitude.toDouble()
    }

    fun getLongtitudeInDouble(): Double {
        return longitude.toDouble()
    }

    fun isHotel(): Boolean {
        return type.equals("hotel", true)
    }

    fun isAirport(): Boolean {
        return type.equals("airport", true)
    }
}