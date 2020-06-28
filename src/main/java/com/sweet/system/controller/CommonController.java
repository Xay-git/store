package com.sweet.system.controller;import cn.hutool.json.JSONUtil;import com.sweet.core.exception.ServiceException;import com.sweet.core.model.ResultBean;import com.sweet.core.model.system.layMenu;import com.sweet.core.model.system.layTree;import com.sweet.core.properties.SystemProperties;import com.sweet.core.shiro.ShiroKit;import com.sweet.system.entity.FileInfo;import com.sweet.system.entity.User;import com.sweet.system.model.XmSelect;import com.sweet.system.service.*;import com.sweet.system.model.UploadResult;import org.apache.commons.lang3.StringUtils;import org.apache.ibatis.annotations.Param;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import java.io.File;import java.util.*;@Controller@RequestMapping("/admin/common")public class CommonController {    @Autowired    UserService userService;    @Autowired    RoleService roleService;    @Autowired    FileService fileService;    @Autowired    MenuService menuService;    @Autowired    FileInfoService fileInfoService;    @RequestMapping("/console")    public String console(){        return "/system/common/console";    }    @RequestMapping("/theme")    public String theme(){        return "/system/common/theme";    }    @RequestMapping("/choose_icon")    public String choose(){        return "/system/common/choose_icon";    }    @RequestMapping("/choose_avatar")    public String chooseAvatar(){        return "/system/common/choose_avatar";    }    /**     * 获得用户所拥有的菜单列表     * @return     */    @RequestMapping("/navTree")    @ResponseBody    public ResultBean navTree(){        User user = ShiroKit.getUser();        List<layMenu> list= userService.findNavByUserName(user.getUserName());        return ResultBean.success(list);    }    @RequestMapping("/getPermissions")    @ResponseBody    public ResultBean getPermissions(){        User user = ShiroKit.getUser();        Set<String> permissions = userService.getPermissions(user.getUserName());        return ResultBean.success(permissions);    }    /**     * 获得角色多选     * @param menu     * @return     */    @RequestMapping("/getXmSelect")    @ResponseBody    public ResultBean getXmSelect(){        List<XmSelect> list = roleService.getXmSelect();        return ResultBean.success(list);    }    /**     * 获得系统用户     * @param user     * @return     */    @RequestMapping("/getAdminUser")    @ResponseBody    public ResultBean getUserList(){        String deptId = ShiroKit.getUser().getDeptId();        List<User> list =  userService.getAdminUser(deptId);        return ResultBean.success(list);    }    /**     * 获得菜单树 配置权限     * @return     */    @RequestMapping("/menuTree")    @ResponseBody    public ResultBean menuTree(){        List<layTree> trees = menuService.getMenuTree();        return ResultBean.success(trees);    }    /**     * layui上传组件 通用文件上传接口     *     * @author fengshuonan     * @Date 2019-2-23 10:48:29     */    @RequestMapping(method = RequestMethod.POST, path = "/upload")    @ResponseBody    public ResultBean layuiUpload(@RequestPart("file") MultipartFile file,String fileSavePath) {        UploadResult uploadResult = fileService.uploadFile(file,fileSavePath);        String fileId = uploadResult.getFileId();        String filePath = uploadResult.getFileSavePath();        HashMap<String, Object> map = new HashMap<>();        map.put("fileId", fileId);        map.put("filePath",filePath);        return ResultBean.success(0, "上传成功", map);    }    @RequestMapping("/getFiles")    @ResponseBody    public ResultBean getFiles(String fileIds){        List<FileInfo> list = fileInfoService.getFileInfoByIds(fileIds);        return ResultBean.success(list);    }    public static boolean deleteFile(String pathname){        boolean result = false;        File file = new File(pathname);        if (file.exists()) {            file.delete();            result = true;            System.out.println("文件已经被成功删除");        }        return result;    }    @RequestMapping( path = "/delFile")    @ResponseBody    public ResultBean delFile(String fileId) {        FileInfo file = fileInfoService.getById(fileId);        String filePath = file.getFileSysPath();        boolean del = deleteFile(filePath);        if(!del){            throw new ServiceException(500,"删除文件失败！");        }        fileInfoService.removeById(file);        return ResultBean.success();    }    @GetMapping("getLoginUser")    @ResponseBody    public ResultBean getLoginUser() {       return ResultBean.success(ShiroKit.getUser());    }}