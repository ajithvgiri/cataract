package com.ajithvgiri.cataract.api

import com.ajithvgiri.cataract.api.model.Response
import io.reactivex.Observable
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiInterface {


    @POST("neuraltalk")
    @FormUrlEncoded
    fun neuraltalk(@FieldMap params: Map<String, String>): Observable<Response>
}