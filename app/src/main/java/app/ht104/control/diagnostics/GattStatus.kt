package app.ht104.control.diagnostics

object GattStatus {
    fun describe(status: Int): String {
        val name = when (status) {
            0 -> "GATT_SUCCESS"
            2 -> "READ_NOT_PERMITTED"
            3 -> "WRITE_NOT_PERMITTED"
            5 -> "INSUFFICIENT_AUTHENTICATION"
            6 -> "REQUEST_NOT_SUPPORTED"
            7 -> "INVALID_OFFSET"
            8 -> "CONNECTION_TIMEOUT"
            13 -> "INVALID_ATTRIBUTE_LENGTH"
            15 -> "INSUFFICIENT_ENCRYPTION"
            19 -> "REMOTE_USER_TERMINATED"
            22 -> "LOCAL_HOST_TERMINATED"
            34 -> "LMP_RESPONSE_TIMEOUT"
            62 -> "CONNECTION_FAILED_TO_ESTABLISH"
            128 -> "NO_RESOURCES"
            129 -> "INTERNAL_ERROR"
            133 -> "GATT_ERROR"
            143 -> "CONNECTION_CONGESTED"
            257 -> "GATT_FAILURE"
            -1 -> "NOT_STARTED_OR_TIMEOUT"
            else -> "UNKNOWN"
        }
        return "$status ($name)"
    }
}
