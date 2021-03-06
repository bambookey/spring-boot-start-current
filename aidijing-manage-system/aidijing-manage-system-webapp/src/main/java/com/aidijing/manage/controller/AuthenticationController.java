package com.aidijing.manage.controller;

import com.aidijing.common.ResponseEntityPro;
import com.aidijing.common.util.ValidatedGroups;
import com.aidijing.manage.bean.dto.UserForm;
import com.aidijing.manage.jwt.JwtUser;
import com.aidijing.manage.permission.Pass;
import com.aidijing.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mobile.device.Device;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 授权认证控制器
 *
 * @author pijingzhanji
 */
@Pass
@RestController
@RequestMapping( "authentication" )
public class AuthenticationController {

	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private JwtTokenUtil          jwtTokenUtil;
	@Autowired
	private UserDetailsService    userDetailsService;

	/**
	 * 认证
	 *
	 * @param user   : 表单
	 * @param device : 终端
	 * @return token
	 * @throws AuthenticationException 认证失败则会抛异常
	 */
	@PostMapping
	public ResponseEntity createAuthenticationToken ( @Validated( ValidatedGroups.Special.class ) @RequestBody UserForm user ,
													  Device device ) throws AuthenticationException {
		// 执行安全认证
		final Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				user.getUsername() ,
				user.getPassword()
			)
		);
		SecurityContextHolder.getContext().setAuthentication( authentication );
		final UserDetails userDetails = ( UserDetails ) authentication.getPrincipal();
		final String      token       = jwtTokenUtil.generateToken( userDetails , device );
		// 返回
		return new ResponseEntityPro().add( "token" , token )
									  .add( "user" , userDetails )
									  .flushBodyByFilterFields(
										  "*,-user.password,-user.lastPasswordResetDate,-user.createTime,-user.updateTime,-user.remark,-user.enabled"
									  ).buildOk();
	}

	/**
	 * 刷新并认证token
	 *
	 * @return token
	 */
	@PutMapping
	public ResponseEntity refreshAndGetAuthenticationToken ( @RequestHeader( "${jwt.header:Authorization}" ) final String token ) {
		String  username = jwtTokenUtil.getUsernameFromToken( token );
		JwtUser user     = ( JwtUser ) userDetailsService.loadUserByUsername( username );
		if ( jwtTokenUtil.canTokenBeRefreshed( token , user.getLastPasswordResetDate() ) ) {
			String refreshedToken = jwtTokenUtil.refreshToken( token );
			return new ResponseEntityPro().add( "token" , refreshedToken ).buildOk();
		} else {
			return ResponseEntityPro.badRequest( "原 token 无效" );
		}
	}

}
