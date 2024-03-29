package com.clab.securecoding.service;

import com.clab.securecoding.auth.jwt.JwtTokenProvider;
import com.clab.securecoding.exception.LoginFaliedException;
import com.clab.securecoding.exception.NotFoundException;
import com.clab.securecoding.exception.PermissionDeniedException;
import com.clab.securecoding.exception.UserLockedException;
import com.clab.securecoding.repository.LoginFailInfoRepository;
import com.clab.securecoding.type.dto.LogInfoRequestDto;
import com.clab.securecoding.type.dto.RefreshTokenDto;
import com.clab.securecoding.type.dto.TokenDto;
import com.clab.securecoding.type.dto.TokenInfo;
import com.clab.securecoding.type.entity.LoginFailInfo;
import com.clab.securecoding.type.entity.User;
import com.clab.securecoding.type.etc.LogType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final JwtTokenProvider jwtTokenProvider;

    private final LoginFailInfoRepository loginFailInfoRepository;

    private final UserService userService;

    private final LogInfoService logInfoService;

    private static final int MAX_LOGIN_FAILURES = 5;

    private static final int LOCK_DURATION_MINUTES = 5;

    @Transactional
    public TokenInfo login(String id, String password, HttpServletRequest request) throws LoginFaliedException, UserLockedException {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(id, password);

        try {
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            LoginFailInfo loginFailInfo = getLoginFailInfoByUserIdOrThrow(authentication.getName());
            if (loginFailInfo == null) {
                User user = userService.getUserByUserIdOrThrow(authentication.getName());
                loginFailInfo = LoginFailInfo.builder()
                        .user(user)
                        .loginFailCount(0L)
                        .isLock(false)
                        .build();
                loginFailInfoRepository.save(loginFailInfo);
            }
            checkUserLocked(loginFailInfo);
            resetLoginFailInfo(loginFailInfo);

            TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

            LogInfoRequestDto logInfoRequestDto = new LogInfoRequestDto(LogType.LOGIN, id, null, request.getRemoteAddr(),"LOW");
            logInfoService.createLogInfo(logInfoRequestDto, request);
            return tokenInfo;
        } catch (BadCredentialsException e) {
            updateLoginFailInfo(id);
        }

        return null;
    }

    public TokenInfo reissue(RefreshTokenDto refreshTokenDto) {
        String refreshToken = refreshTokenDto.getRefreshToken();
        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
            TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);
            return tokenInfo;
        }
        return null;
    }

    public void banUserById(String userId) throws PermissionDeniedException {
        userService.checkUserAdminRole();
        LoginFailInfo loginFailInfo = getLoginFailInfoByUserIdOrThrow(userId);
        if (loginFailInfo != null) {
            loginFailInfo.setIsLock(true);
            loginFailInfo.setLatestTryLoginDate(LocalDateTime.now().plusYears(100));
            loginFailInfoRepository.save(loginFailInfo);
        }
    }

    public void unbanUserById(String userId) throws PermissionDeniedException {
        userService.checkUserAdminRole();
        LoginFailInfo loginFailInfo = getLoginFailInfoByUserIdOrThrow(userId);
        if (loginFailInfo != null) {
            loginFailInfo.setIsLock(false);
            loginFailInfoRepository.save(loginFailInfo);
        }
    }

    public boolean checkTokenRole(TokenDto tokenDto) {
        String token = tokenDto.getToken();
        if (tokenDto != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            if (claims.get("role").toString().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }

    private void checkUserLocked(LoginFailInfo loginFailInfo) throws UserLockedException {
        if (loginFailInfo != null && loginFailInfo.getIsLock() && isLockedForDuration(loginFailInfo)) {
            throw new UserLockedException();
        }
    }

    private boolean isLockedForDuration(LoginFailInfo loginFailInfo) {
        LocalDateTime unlockTime = loginFailInfo.getLatestTryLoginDate().plusMinutes(LOCK_DURATION_MINUTES);
        return LocalDateTime.now().isBefore(unlockTime);
    }

    private void resetLoginFailInfo(LoginFailInfo loginFailInfo) {
        if (loginFailInfo != null) {
            loginFailInfo.setLoginFailCount(0L);
            loginFailInfo.setIsLock(false);
            loginFailInfoRepository.save(loginFailInfo);
        }
    }

    private void updateLoginFailInfo(String userId) throws LoginFaliedException {
        LoginFailInfo loginFailInfo = getLoginFailInfoByUserIdOrThrow(userId);
        if (loginFailInfo != null) {
            incrementFailCountAndLock(loginFailInfo);
        }
        throw new LoginFaliedException();
    }

    private void incrementFailCountAndLock(LoginFailInfo loginFailInfo) {
        loginFailInfo.setLoginFailCount(loginFailInfo.getLoginFailCount() + 1);
        if (loginFailInfo.getLoginFailCount() >= MAX_LOGIN_FAILURES) {
            if (loginFailInfo.getIsLock().equals(false)) {
                loginFailInfo.setLatestTryLoginDate(LocalDateTime.now());
                loginFailInfo.setIsLock(true);
            }
        }
        loginFailInfoRepository.save(loginFailInfo);
    }

    public LoginFailInfo getLoginFailInfoByUserIdOrThrow(String userId) {
        return loginFailInfoRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("해당 유저가 없습니다."));
    }

}
