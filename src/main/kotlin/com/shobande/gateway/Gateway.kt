package com.shobande.gateway

import com.shobande.exceptions.CannotParseRequest

interface Gateway {

    val name: String

    /**
     * Transforms request formats from various gateways into a uniform [Request] object
     *
     * @param request the request received from the gateway
     * When implementing this interface for any gateway, you should define a data class that mirrors the request
     * you expect from the gateway
     * @return [Request] standard request
     *
     * @throws [CannotParseRequest] thrown when an instance of your data class cannoot be generate from [request]
     */
    @Throws(CannotParseRequest::class)
    fun transform(request: Any) : Request

    /**
     * Sends a response without ending the USSD session
     *
     * @param message text to display to the user
     */
    suspend fun con(message: String): Any

    /**
     * Send a response and ends the USSD session
     *
     * @param message text to display to the user
     */
    suspend fun end(message: String): Any
}