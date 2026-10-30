package br.com.sulwork.events.dto;

/** One option of a closed list: the code the API uses and the text the user reads. */
public record OptionResponse(String code, String description) {
}
