package com.aomsir.jewixapi.handler;

import com.aomsir.jewixapi.exception.CustomerException;
import com.aomsir.jewixapi.pojo.entity.User;
import com.aomsir.jewixapi.pojo.vo.LoginVo;
import com.aomsir.jewixapi.utils.HostHolder;
import com.aomsir.jewixapi.utils.JwtUtils;
import com.aomsir.jewixapi.utils.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Aomsir
 * @Date: 2023/2/19
 * @Description: 账号密码登录过滤器
 * @Email: info@say521.cn
 * @GitHub: <a href="https://github.com/aomsir">GitHub</a>
 */
public class EmailPasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private RedisTemplate<String,Object> redisTemplate;


    public EmailPasswordAuthenticationFilter(AuthenticationManager authenticationManager,RedisTemplate<String,Object> redisTemplate) {
        this.setAuthenticationManager(authenticationManager);
        this.redisTemplate = redisTemplate;
        this.setPostOnly(false);
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login","POST"));
    }

    /**
     * 登录验证
     * @param request 请求对象
     * @param response 响应对象
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("请求方式不受支持");
        }
        if (request.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
            try {
                // 使用Spring工具将请求流反序列化
                LoginVo userInfo = new ObjectMapper().readValue(request.getInputStream(), LoginVo.class);

                // TODO: 2.0版本添加校验验证码(邮箱)
                String username = userInfo.getUsername();
                String password = userInfo.getPassword();

                if (!EmailValidator.validate(username)) {
                    // 抛出的异常,会被下面的失败回调处理
                    try {
                        throw new CustomerException("邮箱格式有误");
                    } catch (CustomerException e) {
                        throw new AuthenticationServiceException(e.getMessage());
                    }
                }


                // 生成令牌(调用UserDetailServiceImpl->PasswordEncoder)
                // 成功以后调用下面的回调函
                UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username,password);
                return this.getAuthenticationManager().authenticate(authRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return super.attemptAuthentication(request, response);
    }


    /**
     * 登录成功回调
     * @param req 请求对象
     * @param resp 详情对象
     * @param chain 调用链
     * @param auth 认证对象
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        User user = (User) auth.getPrincipal();

        HashMap<String,String> temp = new HashMap<String,String>(){{
            put("userId", user.getId().toString());
            put("uuid",user.getUuid());
        }};

        String token = JwtUtils.getToken(temp);
        HashMap<String,String> returnToken = new HashMap<String,String>(){{
            put("token",token);
        }};

        this.redisTemplate.opsForValue().set("user:token:"+user.getId().toString(),token,7, TimeUnit.DAYS);
        // TODO:用户权限信息封装进Redis
        R r = R.ok(String.valueOf(returnToken));

        resp.setStatus(HttpStatus.OK.value());
        resp.setContentType("application/json;charset=UTF-8");
        String s = new ObjectMapper().writeValueAsString(r);
        resp.getWriter().println(s);
    }


    /**
     * 登录失败回调
     * @param req 请求对象
     * @param resp 响应对象
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest req,
                                              HttpServletResponse resp,
                                              AuthenticationException e) throws IOException, ServletException {
        R r = R.error(e.getMessage());
        if (e instanceof BadCredentialsException) {
            r = R.error("用户名或密码错误");
        } else if (e instanceof LockedException) {
            r = R.error("账户禁用,请联系管理员");
        } else if (e instanceof AuthenticationServiceException) {
            r = R.error(e.getMessage());   // TODO:邮箱格式有误(自己对SpringSecurity进行一层封装)
        }

        resp.setStatus(HttpStatus.OK.value());

        resp.setContentType("application/json;charset=UTF-8");
        String s = new ObjectMapper().writeValueAsString(r);
        resp.getWriter().println(s);
    }
}
