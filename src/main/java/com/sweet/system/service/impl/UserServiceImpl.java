package com.sweet.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sweet.core.exception.ServiceException;
import com.sweet.core.model.system.LayuiPageFactory;
import com.sweet.core.model.system.LayuiPageInfo;
import com.sweet.core.model.system.layMenu;
import com.sweet.core.shiro.ShiroKit;
import com.sweet.core.util.MD5Utils;
import com.sweet.core.util.RedisUtil;
import com.sweet.core.util.StringUtil;
import com.sweet.system.entity.Menu;
import com.sweet.system.entity.Role;
import com.sweet.system.entity.User;
import com.sweet.system.entity.UserRole;
import com.sweet.system.mapper.MenuMapper;
import com.sweet.system.mapper.RoleMapper;
import com.sweet.system.mapper.UserMapper;
import com.sweet.system.service.FileInfoService;
import com.sweet.system.service.UserRoleService;
import com.sweet.system.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sweet.core.SystemConst.*;
import static com.sweet.core.exception.enums.SystemExceptionEnum.ACCOUNT_ALREADY_EXCEPTION;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wxl
 * @since 2019-06-20
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserRoleService userRoleService;

    @Autowired
    MenuMapper menuMapper;

    @Autowired
    RoleMapper roleMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    FileInfoService fileInfoService;

    @Override
    public User addUser(User user) {
        String username = user.getUserName().toLowerCase();
        User tempUser = findByUserName(username);
        if(tempUser!=null){
            throw new ServiceException(ACCOUNT_ALREADY_EXCEPTION);
        }
        user.setUserName(username);
        user.setPassword(MD5Utils.encrypt(username,DEFAULT_PASSWORD));
        user.setAccountStatus(USER_NORMAL);
        save(user);
        return user;
    }

    @Override
    public User editUser(User user) {
        String username = user.getUserName().toLowerCase();
        User temp = findByUserName(username);
        if((temp!=null)&&!temp.getUserId().equals(user.getUserId())){
            throw new ServiceException(ACCOUNT_ALREADY_EXCEPTION);
        }
        String password = MD5Utils.encrypt(user.getUserName(), User.DEFAULT_PASSWORD);
        user.setPassword(password);
        updateById(user);
        return user;
    }

    @Override
    public User findByUserName(String userName) {
        userName = userName.toLowerCase();
        return userMapper.findByUserName(userName);
    }

    @Override
    public User findByUserNo(String userNo, String deptId) {
        return baseMapper.findByUserNo(userNo,deptId);
    }

    @Override
    public Set<String> getPermissions(String userName) {
        return menuMapper.findPermissionsByUserName(userName);
    }

    @Override
    public LayuiPageInfo findPageBySpec(User user) {
        Page pageContext = getPageContext();
        IPage page = this.baseMapper.customPageList(pageContext, user);
        return LayuiPageFactory.createPageInfo(page);
    }

    @Override
    public User findUserById(String id) {
        User user = baseMapper.findByUserId(id);
        if(StringUtil.isEmpty(user.getAvatar())){
            user.setAvatarPath(DEFALT_AVATAR);
        }
        return user;
    }

    @Override
    public void setRoleAssign(String userId, String roleIds) {
        baseMapper.deleteRoleAssignById(userId);
        String[] ids = roleIds.split(",");
        for(String roleId:ids){
            UserRole userRole = new UserRole();
            userRole.setUid(userId);
            userRole.setRid(roleId);
            userRoleService.save(userRole);
        }
    }

    @Override
    public List<String> getRoleByUserId(String userId) {
        return baseMapper.getRoleByUserId(userId);
    }

    @Override
    public Set<String> getUserRole(String userName) {
        User user = ShiroKit.getUser();
        Set<String> roleSet = (Set<String>) redisUtil.get("user:role:"+userName);
        if(roleSet==null){
            List<Role> roleList = roleMapper.findRoleByUserName(userName);
            roleSet = roleList.stream().map(Role::getName).collect(Collectors.toSet());
            redisUtil.set("user:role:"+userName,roleSet);
        }
        return roleSet;
    }

    @Override
    public Set<String> getUserMenu(String userName) {
        User user = ShiroKit.getUser();
        Set<String> menuSet = (Set<String>) redisUtil.get("user:menu:"+userName);
        if(menuSet==null){
            // 获取用户菜单url集合
            List<Menu> menuList = menuMapper.findMenuByUserName(userName);
            menuSet = menuList.stream().map(Menu::getUrl).collect(Collectors.toSet());
            redisUtil.set("user:menu:"+userName,menuSet);
        }
        return menuSet;
    }

    @Override
    public List<layMenu> findNavByUserName(String userName) {
        List<layMenu> list = menuMapper.findNavByUserName(userName);
        ArrayList<layMenu> trees = (ArrayList<layMenu>) list.stream().distinct().collect(Collectors.toList());
        ArrayList<layMenu> cloneTree = (ArrayList<layMenu>) trees.clone();
        ArrayList<layMenu> newtrees = new ArrayList<layMenu>();

        if(trees.size()>0){
            for (int i= 0;i<trees.size();i++){
                layMenu menu = trees.get(i);
                if(menu.getParentId().equals("0")){
                    newtrees.add(menu);
                    cloneTree.remove(menu);
                }
            }
            if(cloneTree.size()>0){
                newtrees = coverMenu(newtrees,cloneTree);
            }
        }
        return newtrees;
    }

    @Override
    public List<User> getAdminUser(String deptId) {
        return baseMapper.getAdminUser(deptId);
    }

    @Override
    public LayuiPageInfo getTechnicians(String deptId) {
        Page pageContext = getPageContext();
        IPage page = this.baseMapper.getTechnicians(pageContext, deptId);
        return LayuiPageFactory.createPageInfo(page);
    }


    public ArrayList<layMenu> coverMenu(ArrayList<layMenu> trees, ArrayList<layMenu> tempTrees){
        if(trees.size()==0)return trees;
        if(tempTrees.size()==0)return tempTrees;
        ArrayList<layMenu> layTrees = new ArrayList<layMenu>();
        ArrayList<layMenu> tempLayTrees = (ArrayList<layMenu>) tempTrees.clone();
        for(layMenu node:trees){
            for(layMenu temp:tempTrees){
                if(temp.getParentId().equals(node.getMenuId())){
                    if(node.getList()==null){
                        node.setList(new ArrayList<layMenu>());
                    }
                    node.getList().add(temp);
                    layTrees.add(temp);
                    tempLayTrees.remove(temp);
                }
            }
        }
        coverMenu(layTrees,tempLayTrees);
        return trees;
    }

    private Page getPageContext() {
        return LayuiPageFactory.defaultPage();
    }
}
