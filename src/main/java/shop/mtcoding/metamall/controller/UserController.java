package shop.mtcoding.metamall.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import shop.mtcoding.metamall.core.exception.Exception400;
import shop.mtcoding.metamall.core.exception.Exception401;
import shop.mtcoding.metamall.core.jwt.JwtProvider;
import shop.mtcoding.metamall.dto.ResponseDto;
import shop.mtcoding.metamall.dto.user.UserRequest;
import shop.mtcoding.metamall.dto.user.UserRegister;
import shop.mtcoding.metamall.model.log.login.LoginLog;
import shop.mtcoding.metamall.model.log.login.LoginLogRepository;
import shop.mtcoding.metamall.model.user.User;
import shop.mtcoding.metamall.model.user.UserRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserRepository userRepository;
    private final LoginLogRepository loginLogRepository;
    private final HttpSession session;

    @PostMapping("/user/login")
    public ResponseEntity<?> login(@RequestBody UserRequest.LoginDto loginDto, HttpServletRequest request) {
        Optional<User> userOP = userRepository.findByUsername(loginDto.getUsername());
        if (false == userOP.isPresent())
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>().fail(HttpStatus.BAD_REQUEST, "존재하지 않는 ID 입니다", ""));
        }

        // 1. 유저 정보 꺼내기
        User loginUser = userOP.get();

        // 2. 패스워드 검증하기
        if(!loginUser.getPassword().equals(loginDto.getPassword()))
        {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>().fail(HttpStatus.BAD_REQUEST, "유저네임 혹은 패스워드가 잘못되었습니다", ""));
        }

        // 3. JWT 생성하기
        String jwt = JwtProvider.create(userOP.get());

        // 4. 최종 로그인 날짜 기록 (더티체킹 - update 쿼리 발생)
        loginUser.setUpdatedAt(LocalDateTime.now());

        // 5. 로그 테이블 기록
        LoginLog loginLog = LoginLog.builder()
                .userId(loginUser.getId())
                .userAgent(request.getHeader("User-Agent"))
                .clientIP(request.getRemoteAddr())
                .build();
        loginLogRepository.save(loginLog);

        // 6. 응답 DTO 생성
        ResponseDto<?> responseDto = new ResponseDto<>().data(loginUser);
        return ResponseEntity.ok().header(JwtProvider.HEADER, jwt).body(responseDto);

    }

    @Transactional
    @PostMapping("/user/register")
    public ResponseEntity<?> register(@RequestBody UserRegister userRegister)
    {
        Optional<User> existingUser = userRepository.findByUsername(userRegister.getUsername());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>().fail(HttpStatus.BAD_REQUEST, "등록정보가 있습니다.", ""));
            //throw new Exception401("등록정보가 있습니다");
        }

        // If the user doesn't exist, create a new one
        User newUser = new User();
        newUser.setUsername(userRegister.getUsername());
        newUser.setPassword(userRegister.getPassword());
        newUser.setEmail(userRegister.getEmail());

        newUser.setCreatedAt(LocalDateTime.now());

        User registeredUser = userRepository.save(newUser);
        ResponseDto<?> responseDto = new ResponseDto<>().data(registeredUser);

        return ResponseEntity.ok().body(responseDto);
    }
}
