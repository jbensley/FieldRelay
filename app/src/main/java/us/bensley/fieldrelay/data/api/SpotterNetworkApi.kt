package us.bensley.fieldrelay.data.api

import us.bensley.fieldrelay.data.model.PositionUpdate
import us.bensley.fieldrelay.data.model.SevereReportRequest
import us.bensley.fieldrelay.data.model.UpdateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SpotterNetworkApi {
    @POST("positions/update")
    suspend fun updatePosition(@Body body: PositionUpdate): UpdateResponse

    @POST("report/severe")
    suspend fun submitSevereReport(@Body body: SevereReportRequest): Response<Unit>
}
