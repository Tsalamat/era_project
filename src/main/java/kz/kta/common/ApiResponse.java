package kz.kta.common;

public record ApiResponse(String status, String message) {

	public static ApiResponse accepted(String message) {
		return new ApiResponse("accepted", message);
	}
}
