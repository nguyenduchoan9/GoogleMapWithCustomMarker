package solution.com.googlemap

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface POIApi {
    @GET("pois")
    fun fetchPOI(@Query("sw") sw: String,
                 @Query("ne") ne: String): Call<List<POI>>
}