package com.mcardoso.srvcondominiopro.modules.auth.dto;

// Resposta neutra para fluxos que não devem revelar se um dado existe (ex.: forgot-password).
public record MessageResponse(String message) {
}
