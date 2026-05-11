package com.sl.rag4j.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sl.rag4j.entity.*;
import com.sl.rag4j.mapper.*;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserManageMentProcess {

	@Resource
	private UserMapper userMapper;
	@Resource
	private RoleMapper roleMapper;
	@Resource
	private UsersRoleMapper usersRoleMapper;
	@Resource
	private PermissionMapper permissionMapper;
	@Resource
	private RolesPermissionMapper rolesPermissionMapper;
	@Resource
	private PasswordEncoder passwordEncoder;

	/**
	 * 插入用户（选择性插入null字段）
	 */
	public void insertSelective(User user) {
		userMapper.insert(user);
	}

	/**
	 * 根据用户ID查询用户
	 */
	public User selectUserById(String userId) {
		return userMapper.selectById(userId);
	}

	/**
	 * 查询所有用户
	 */
	public List<User> selectAllUser() {
		return userMapper.selectList(new QueryWrapper<User>());
	}

	/**
	 * 根据机构ID查询用户列表
	 */
	public List<User> selectUserByIdInstitution(String idInstitution) {
		LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(User::getIdInstitution, idInstitution);
		return userMapper.selectList(wrapper);
	}

	/**
	 * 根据用户ID更新用户信息（选择性更新）
	 */
	public void updateUserByUserId(User user) {
		userMapper.updateById(user);
	}

	/**
	 * 根据用户ID集合查询用户
	 */
	public List<User> getUserByIds(Set<String> userIds) {
		List<String> ids = new ArrayList<>(userIds);
		LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
		wrapper.in(User::getUserId, ids);
		return userMapper.selectList(wrapper);
	}

	/**
	 * 根据用户ID删除用户
	 */
	public void deleteUserById(String userId) {
		userMapper.deleteById(userId);
	}

	/**
	 * 根据用户ID查询关联的角色ID列表
	 */
	public List<String> selectRoleIdsByUserId(String userId) {
		LambdaQueryWrapper<UsersRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UsersRole::getUserId, userId);
		return usersRoleMapper.selectList(wrapper)
				.stream().map(UsersRole::getRoleId).collect(Collectors.toList());
	}

	/**
	 * 根据角色ID查询关联的用户ID列表
	 */
	public List<String> selectUserIdByRoleId(String roleId) {
		LambdaQueryWrapper<UsersRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UsersRole::getRoleId, roleId);
		return usersRoleMapper.selectList(wrapper)
				.stream().map(UsersRole::getUserId).collect(Collectors.toList());
	}

	/**
	 * 根据角色ID查询用户列表
	 */
	public List<User> selectUsersByRoleId(String roleId) {
		List<String> userIds = selectUserIdByRoleId(roleId);
		if (userIds == null || userIds.isEmpty()) {
			return new ArrayList<>();
		}
		LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
		wrapper.in(User::getUserId, userIds);
		return userMapper.selectList(wrapper);
	}

	/**
	 * 根据用户ID查询角色列表
	 */
	public List<Role> selectRolesByUserId(String userId) {
		LambdaQueryWrapper<UsersRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UsersRole::getUserId, userId);
		List<String> roleIds = usersRoleMapper.selectList(wrapper)
				.stream().map(UsersRole::getRoleId).collect(Collectors.toList());
		if (roleIds.isEmpty()) {
			return new ArrayList<>();
		}
		LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
		roleWrapper.in(Role::getRoleId, roleIds);
		return roleMapper.selectList(roleWrapper);
	}

	/**
	 * 删除角色与权限的关联关系
	 */
	public int deleteRolePermissionByRoleId(String roleId) {
		LambdaQueryWrapper<RolesPermission> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RolesPermission::getRoleId, roleId);
		return rolesPermissionMapper.delete(wrapper);
	}

	/**
	 * 查询用户数量
	 */
	public Long selectUserCounts(String platform) {
		return userMapper.selectCount(new QueryWrapper<User>());
	}

	/**
	 * 查询所有权限，按排序字段排序
	 */
	public List<Permission> selectAllPermissions() {
		LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
		wrapper.orderByAsc(Permission::getNbrOrder);
		return permissionMapper.selectList(wrapper);
	}

	/**
	 * 根据权限ID查询权限
	 */
	public Permission selectPermissionById(String permissionID) {
		return permissionMapper.selectById(permissionID);
	}

	/**
	 * 插入用户角色关联
	 */
	public void insertUserRole(String userId, String roleId) {
		usersRoleMapper.insert(new UsersRole(userId, roleId));
	}

	/**
	 * 根据用户ID删除用户角色关联
	 */
	public void deleUserRoleByUserId(String userId) {
		LambdaQueryWrapper<UsersRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UsersRole::getUserId, userId);
		usersRoleMapper.delete(wrapper);
	}

	/**
	 * 插入角色（选择性插入）
	 */
	public void insertRole(Role role) {
		roleMapper.insert(role);
	}

	/**
	 * 根据角色ID查询关联的权限ID列表
	 */
	public List<String> selectPermissionIdsByRoleId(String roleId) {
		LambdaQueryWrapper<RolesPermission> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RolesPermission::getRoleId, roleId);
		return rolesPermissionMapper.selectList(wrapper)
				.stream().map(RolesPermission::getPermissionId).collect(Collectors.toList());
	}

	/**
	 * 根据角色ID查询权限列表
	 */
	public List<Permission> selectPermissionByRoleId(String roleId) {
		LambdaQueryWrapper<RolesPermission> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RolesPermission::getRoleId, roleId);
		List<String> permissionIds = rolesPermissionMapper.selectList(wrapper)
				.stream().map(RolesPermission::getPermissionId).collect(Collectors.toList());
		if (permissionIds.isEmpty()) {
			return new ArrayList<>();
		}
		LambdaQueryWrapper<Permission> permWrapper = new LambdaQueryWrapper<>();
		permWrapper.in(Permission::getPermissionId, permissionIds);
		return permissionMapper.selectList(permWrapper);
	}

	/**
	 * 插入角色权限关联
	 */
	public void insertRolePermission(String roleId, String permissionId) {
		rolesPermissionMapper.insert(new RolesPermission(roleId, permissionId));
	}

	/**
	 * 查询所有角色
	 */
	public List<Role> getAllRoles() {
		return roleMapper.selectList(new QueryWrapper<Role>());
	}

	/**
	 * 检查角色是否有用户关联
	 */
	public boolean checkRoleUser(String roleId) {
		LambdaQueryWrapper<UsersRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UsersRole::getRoleId, roleId);
		Long count = usersRoleMapper.selectCount(wrapper);
		return count != null && count > 0;
	}

	/**
	 * 删除角色及其权限关联
	 */
	public void deleteRole(String roleId) {
		deleteRolePermissionByRoleId(roleId);
		deleteRoleByRoleId(roleId);
	}

	/**
	 * 根据角色ID删除角色
	 */
	private void deleteRoleByRoleId(String roleId) {
		roleMapper.deleteById(roleId);
	}

	/**
	 * 更新用户角色关联（先删后增）
	 */
	public void updateUserRoleByUserId(String userId, String roleId) {
		deleUserRoleByUserId(userId);
		insertUserRole(userId, roleId);
	}

	/**
	 * 根据用户ID查询所有权限
	 */
	public List<Permission> selectAllPermissionsByIdUser(String idUser) {
		List<Permission> result = new ArrayList<>();
		try {
			if (idUser != null) {
				List<String> idRoles = selectRoleIdsByUserId(idUser);
				if (idRoles != null && !idRoles.isEmpty()) {
					for (String roleId : idRoles) {
						List<String> idPermissions = selectPermissionIdsByRoleId(roleId);
						if (idPermissions != null && !idPermissions.isEmpty()) {
							for (String idPermi : idPermissions) {
								Permission permission = permissionMapper.selectById(idPermi);
								if (permission != null) {
									result.add(permission);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 插入角色及其权限关联
	 */
	public void insertRoleAndPermissions(Role role, HashSet<String> permissionIds) {
		roleMapper.insert(role);
		for (String pid : permissionIds) {
			rolesPermissionMapper.insert(new RolesPermission(role.getRoleId(), pid));
		}
	}

	/**
	 * 更新角色权限关联（先删后增）
	 */
	public void updateRoleAndPermissions(String selectedRoleId, HashSet<String> permissionIds) {
		LambdaQueryWrapper<RolesPermission> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RolesPermission::getRoleId, selectedRoleId);
		rolesPermissionMapper.delete(wrapper);
		permissionIds.forEach(pid -> rolesPermissionMapper.insert(new RolesPermission(selectedRoleId, pid)));
	}

	/**
	 * 根据用户ID查询用户
	 */
	public User getUserById(String userId) {
		LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(User::getUserId, userId);
		List<User> list = userMapper.selectList(wrapper);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * 根据用户ID查询用户角色关联
	 */
	public UsersRole getRoleByUserId(String userId) {
		LambdaQueryWrapper<UsersRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(UsersRole::getUserId, userId);
		List<UsersRole> list = usersRoleMapper.selectList(wrapper);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * 根据角色ID查询角色权限关联
	 */
	public List<RolesPermission> getRolePermissionByRoleId(String roleId) {
		LambdaQueryWrapper<RolesPermission> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RolesPermission::getRoleId, roleId);
		return rolesPermissionMapper.selectList(wrapper);
	}

	/**
	 * 对明文密码进行加密
	 */
	public String getPassword(String passed) {
		return passwordEncoder.encode(passed);
	}
}
