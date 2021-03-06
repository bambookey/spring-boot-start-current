package com.aidijing.manage;


import com.aidijing.common.exception.ForbiddenException;
import com.aidijing.manage.bean.domain.Role;
import com.aidijing.manage.bean.domain.RolePermissionResource;
import com.aidijing.manage.bean.domain.User;
import com.aidijing.manage.bean.domain.enums.RoleType;
import com.aidijing.manage.bean.vo.PermissionResourceVO;
import com.aidijing.manage.jwt.JwtUser;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 上下文
 *
 * @author : 披荆斩棘
 * @date : 2017/6/20
 */
public final class ContextUtils {


	private static final SimpleGrantedAuthority                ROLE_ANONYMOUS                           = new SimpleGrantedAuthority(
		"ROLE_ANONYMOUS" );
	private static final String                                ANONYMOUS_USER_PRINCIPAL                 = "anonymousUser";
	/** 当前请求API能查看到的字段 **/
	private static final ThreadLocal< RolePermissionResource > CURRENT_REQUEST_ROLE_PERMISSION_RESOURCE = new ThreadLocal<>();

	/**
	 * 得到用户当前请求api可见字段
	 *
	 * @return
	 */
	public static RolePermissionResource getCurrentRequestRolePermissionResource () {
		return CURRENT_REQUEST_ROLE_PERMISSION_RESOURCE.get();
	}

	/**
	 * 设置用户当前请求api可见字段
	 *
	 * @param currentRequestRolePermissionResource {@link RolePermissionResource}
	 */
	public static void setCurrentRequestRolePermissionResource (
		final RolePermissionResource currentRequestRolePermissionResource ) {
		CURRENT_REQUEST_ROLE_PERMISSION_RESOURCE.set( currentRequestRolePermissionResource );
	}

	/**
	 * 删除用户当前请求api可见字段
	 */
	public static void removeCurrentRequestRolePermissionResource () {
		CURRENT_REQUEST_ROLE_PERMISSION_RESOURCE.remove();
	}


	/**
	 * 是否登录
	 * <p>
	 * {@link Authentication#isAuthenticated()}
	 *
	 * @return <code>true</code> , 匿名用户(未登录)返回 <code>false</code>
	 */
	public static boolean isLogin () {
		Authentication authentication = getAuthentication();
		if ( authentication.getAuthorities().contains( ROLE_ANONYMOUS )
			|| ANONYMOUS_USER_PRINCIPAL.equals( authentication.getPrincipal() ) ) {
			return false;
		}
		return authentication.isAuthenticated();
	}

	/**
	 * ! {@link #isLogin()}
	 *
	 * @return
	 */
	public static boolean isNotLogin () {
		return ! isLogin();
	}

	/**
	 * 得到jwt对象
	 *
	 * @return
	 */
	public static JwtUser getJwtUser () {
		return ( JwtUser ) getAuthentication().getPrincipal();
	}


	/**
	 * 得到 PermissionResourceVO
	 *
	 * @return
	 */
	public static List< PermissionResourceVO > getPermissionResource () {
		return getJwtUser().getPermissionResource();
	}

	/**
	 * 得到User对象
	 *
	 * @return
	 */
	public static User getUser () {
		final JwtUser jwtUser = getJwtUser();
		User          user    = new User();
		BeanUtils.copyProperties( jwtUser , user );
		return user;
	}

	/**
	 * 得到用户id
	 *
	 * @return {@link User#id}
	 */
	public static Long getUserId () {
		return getJwtUser().getId();
	}

	/**
	 * 得到用户详细信息
	 *
	 * @return {@link UserDetails}
	 */
	public static UserDetails getUserDetails () {
		return ( UserDetails ) getAuthentication().getPrincipal();
	}


	/**
	 * 得到凭证
	 */
	private static Authentication getAuthentication () {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if ( Objects.isNull( authentication ) ) {
			throw new AuthenticationCredentialsNotFoundException( "未授权" );
		}
		return authentication;
	}


	/**
	 * 如果当前用户角色是超级管理员 {@link RoleType#SUPER_ADMIN} ,{@link RoleType#ROOT}
	 */
	public static boolean currentUserIsSuperAdmin () {
		return getJwtUser().getRoles().parallelStream().anyMatch( ContextUtils::roleIsSuperAdmin );
	}

	/**
	 * ! {@link #currentUserIsSuperAdmin()}
	 */
	public static boolean currentUserIsNotSuperAdmin () {
		return ! currentUserIsSuperAdmin();
	}

	/**
	 * 如果当前用户角色是ROOT角色
	 *
	 * @throws ForbiddenException
	 */
	public static boolean currentUserIsRoot () {
		return getJwtUser().getRoles().parallelStream().anyMatch( ContextUtils::roleIsRoot );
	}


	/**
	 * 如果当前用户角色是ROOT角色
	 *
	 * @throws ForbiddenException
	 */
	public static boolean isRoot ( Long roleId ) {
		return getJwtUser().getRoles().parallelStream()
						   .filter( role -> Objects.equals( role.getId() , roleId ) ).findAny().orElse( new Role() )
						   .getRoleType().getValue().equals( RoleType.ROOT.getValue() );
	}


	/**
	 * ! {@link #currentUserIsRoot()}
	 */
	public static boolean currentUserNotIsRoot () {
		return ! currentUserIsRoot();
	}


	public static boolean isNotCurrentUser ( Long userId ) {
		return ! isCurrentUser( userId );
	}


	/**
	 * 未登录
	 *
	 * @throws ForbiddenException
	 */
	public static void assertNotLogin () throws ForbiddenException {
		if ( isNotLogin() ) {
			throw new AuthenticationCredentialsNotFoundException( "未登录" );
		}
	}

	/**
	 * 现在是每个Mapping中进行判断,后续做成在拦截器中进行处理
	 *
	 * @param userId
	 * @throws ForbiddenException
	 */
	public static void assertNotCurrentUser ( Long userId ) throws ForbiddenException {
		if ( isNotCurrentUser( userId ) ) {
			throw new ForbiddenException( "非法越权操作,非当前用户ID" );
		}
	}

	private static boolean isCurrentUser ( Long userId ) {
		return Objects.equals( getUserId() , userId );
	}


	/**
	 * 如果当前用户角色不是超级管理员,那么属于越权
	 *
	 * @throws ForbiddenException
	 */
	public static void assertCurrentUserNotSuperAdmin () throws ForbiddenException {
		if ( currentUserIsNotSuperAdmin() ) {
			throw new ForbiddenException( "非超级管理员,不能进行此操作" );
		}
	}


	/**
	 * 如果不是ROOT角色
	 *
	 * @throws ForbiddenException
	 */
	public static void assertCurrentUserNotRoot () throws ForbiddenException {
		if ( currentUserNotIsRoot() ) {
			throw new ForbiddenException( "非ROOT,不能进行此操作" );
		}
	}

	/**
	 * 不是当前用户 角色资源中间表信的 ID
	 *
	 * @param rolePermissionResourceId : {@link RolePermissionResource#id}
	 */
	public static void assertNotCurrentUserRolePermissionResources ( Long rolePermissionResourceId ) {
		final boolean isRolePermissionResource = getJwtUser().getRolePermissionResources()
															 .parallelStream()
															 .anyMatch( rolePermissionResource -> Objects
																 .equals(
																	 rolePermissionResource.getId() ,
																	 rolePermissionResourceId
																 ) );
		if ( ! isRolePermissionResource ) {
			throw new ForbiddenException( "非法越权操作" );
		}

	}


	/**
	 * 是当前用户角色
	 */
	public static boolean isCurrentUserRole ( Long roleId ) {
		return getJwtUser().getRoles().parallelStream()
						   .anyMatch( role -> Objects.equals( role.getId() , roleId ) );
	}

	/**
	 * 不是当前用户角色
	 */
	public static boolean notIsCurrentUserRole ( Long roleId ) {
		return ! isCurrentUserRole( roleId );
	}


	/**
	 * 不是当前用户角色
	 */
	public static void assertNotIsCurrentUserRole ( Long roleId ) {
		if ( notIsCurrentUserRole( roleId ) ) {
			throw new ForbiddenException( "非法越权操作" );
		}
	}


	// 超级管理员
	private static boolean roleIsSuperAdmin ( Role role ) {
		final boolean isSuperAdmin =
			Objects.equals( role.getRoleType().getValue() , RoleType.SUPER_ADMIN.getValue() );
		return isSuperAdmin || roleIsRoot( role );
	}

	// ROOT
	private static boolean roleIsRoot ( Role role ) {
		return Objects.equals( role.getRoleType().getValue() , RoleType.ROOT.getValue() );
	}

	/**
	 * 得到当前用户角色列表
	 *
	 * @return
	 */
	public static List< Role > listCurrentRoles () {
		return ContextUtils.getJwtUser().getRoles();
	}

	/**
	 * 得到当前用户角色ID列表
	 *
	 * @return
	 */
	public static List< Long > listCurrentRoleIds () {
		return listCurrentRoles().parallelStream().map( Role::getId ).collect( Collectors.toList() );
	}
}


