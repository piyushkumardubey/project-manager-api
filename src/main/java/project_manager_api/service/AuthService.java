package project_manager_api.service;

import project_manager_api.dto.LoginRequest;
import project_manager_api.dto.SignupRequest;
import project_manager_api.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}