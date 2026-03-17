package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.Usuario;
import com.suporte.suporte_whatsapp.repository.UsuarioRepository;
import com.suporte.suporte_whatsapp.security.JwtUtil;
import com.suporte.suporte_whatsapp.security.LoginAttemptService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        // ── 1. Verificar bloqueio ANTES de qualquer operação ──────────────────
        if (loginAttemptService.estaBloqueado(req.getEmail())) {
            long minutosRestantes = loginAttemptService.minutosRestantes(req.getEmail());
            throw new LockedException("Conta bloqueada por excesso de tentativas. Tente novamente em "
                    + minutosRestantes + " minuto(s).");
        }
        // ── 2. Tentar autenticação ────────────────────────────────────────────
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getSenha()));
        } catch (BadCredentialsException | DisabledException ex) {
            loginAttemptService.registrarFalha(req.getEmail());
            throw ex; // Re-throw para que o cliente receba a resposta adequada (401 ou 403)
        }
        // ── 3. Autenticação bem-sucedida ──────────────────────────────────────
        loginAttemptService.registrarSucesso(req.getEmail());

        UserDetails ud = userDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtUtil.generateToken(ud);
        Usuario u = usuarioRepository.findByEmail(req.getEmail()).orElseThrow();
        return ResponseEntity.ok(new LoginResponse(token,
                new LoginResponse.UsuarioInfo(u.getId(), u.getNome(), u.getPerfil(), u.getEmail())));
    }
}