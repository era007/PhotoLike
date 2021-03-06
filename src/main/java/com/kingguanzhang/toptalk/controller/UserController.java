package com.kingguanzhang.toptalk.controller;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingguanzhang.toptalk.component.QiniuStatus;
import com.kingguanzhang.toptalk.entity.City;
import com.kingguanzhang.toptalk.entity.Constant;
import com.kingguanzhang.toptalk.entity.Msg;
import com.kingguanzhang.toptalk.entity.User;
import com.kingguanzhang.toptalk.service.CityServiceImpl;
import com.kingguanzhang.toptalk.service.UserServiceImpl;
import com.kingguanzhang.toptalk.utils.Base64ToMultipartUtil;
import com.kingguanzhang.toptalk.utils.ImgUtil;
import com.kingguanzhang.toptalk.utils.PathUtil;
import com.kingguanzhang.toptalk.utils.QiniuCloudUtil;
import com.kingguanzhang.toptalk.utils.RequestUtil;

@Controller
public class UserController {

	@Autowired
	private UserServiceImpl userService;
	@Autowired
	private CityServiceImpl cityService;
	private Logger logger =  LoggerFactory.getLogger(UserController.class);

	/**
	 * 修改用户密码;
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/user/password", method = RequestMethod.POST)
	@ResponseBody
	private Msg editPassword(HttpServletRequest request) {
		// 从session中取出user;
		if (null == request.getSession().getAttribute("user")) {
			// 判断session失效后返回登录界面
			return Msg.fail().setMsg("操作失败,请重新登录后再试!");
		}
		User user = (User) request.getSession().getAttribute("user");
		// 验证原始密码;
		String oldPassword = request.getParameter("oldPassword");
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
		// 将原来的数据库中取出的加密后的密码与页面传来的原始密码进行对比;
		boolean matches = bCryptPasswordEncoder.matches(oldPassword, user.getPassword());
		if (!matches) {
			return Msg.fail().setMsg("原始密码验证未通过,如果您忘记了密码,可以通过联系客服获得帮助 !");
		}
		// 验证新密码的是否符合要求;
		String newPassword = request.getParameter("newPassword");
		if (null == newPassword || "".equals(newPassword.trim()) || 5 > newPassword.trim().length()
				|| 30 < newPassword.length() || oldPassword.equals(newPassword)) {
			return Msg.fail().setMsg("请输入符合要求的新密码 ! ");
		} else {
			// 修改成新的密码;
			user.setPassword(bCryptPasswordEncoder.encode(newPassword)); // 使用security推荐的加密方式加密密码;
			try {
				// 保存到数据库中;
				userService.save(user);
			} catch (Exception e) {
				if (logger.isErrorEnabled()) {
					logger.error("保存用户信息时出现异常: 异常信息:"+ e.getMessage());
				}
				return Msg.fail().setMsg("修改失败,保存用户信息时出现错误!");
			}
		}
		// 返回最终结果;
		return Msg.success().setMsg("修改成功!");

	}

	/**
	 * 修改用户信息
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/user/info", method = RequestMethod.POST)
	@ResponseBody
	private Msg editInfo(HttpServletRequest request) {
		// 从session中取出user;
		if (null == request.getSession().getAttribute("user")) {
			// 判断session失效后返回登录界面
			return Msg.fail().setMsg("操作失败,请重新登录后再试!");
		}
		User user = (User) request.getSession().getAttribute("user");
		/* 设置昵称,需要注意判断用户是否真正的修改了昵称 */
		if (null != request.getParameter("nickname") && !user.getNickname().equals(request.getParameter("nickname"))) {
			String nickname = request.getParameter("nickname");
			if (!checkNickname(nickname) || 16 < user.getNickname().length()) {// 检查昵称是否被占用同时限制长度;
				return Msg.fail().setMsg("昵称已经被占用;");
			}
			user.setNickname(nickname);
		}
		/* 设置城市 */
		City city = new City();
		if (null != request.getParameter("cityId")) {
			String cityId = request.getParameter("cityId");
			city.setId(Long.parseLong(cityId));
			user.setCity(city);
		}
		/* 设置性别 */
		if (null != request.getParameter("gender")) {
			String gender = request.getParameter("gender");
			user.setGender(Integer.valueOf(gender));
		}
		try {
			userService.save(user);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("保存用户信息时出现异常: 异常信息:"+ e.getMessage());
			}
			return Msg.fail().setMsg("修改失败,保存用户信息时出现错误!");
		}
		// 返回最终结果;
		return Msg.success().setMsg("修改成功!");

	}

	/**
	 * 修改签名;
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/user/signature/add", method = RequestMethod.POST)
	@ResponseBody
	private Msg userSignatureAdd(HttpServletRequest request) {
		if (null == request.getSession().getAttribute("user")) {
			// 判断session失效后返回登录界面
			return Msg.fail().setMsg("操作失败,请重新登录后再试!");
		}
		User user = (User) request.getSession().getAttribute("user");
		String signature = request.getParameter("signature");
		if (50 < signature.length()) {
			return Msg.fail().setMsg("签名超出字数限制!");
		}
		user.setSignature(signature);
		try {
			userService.save(user);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("保存用户信息时出现异常: 异常信息:"+ e.getMessage());
			}
			return Msg.fail().setMsg("修改失败!");
		}
		return Msg.success().setMsg("修改签名成功!");
	}

	/**
	 * 修改用户头像,base64格式的字符串转成MultipartFile;
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/user/headImgUpload", method = RequestMethod.POST)
	@ResponseBody
	private Msg testBase64HeadImgUpload(HttpServletRequest request) throws IOException {
		if (null == request.getSession().getAttribute("user")) {
			// 设置错误代码为101,前端判断是此代码时跳转到登录界面;
			return Msg.fail().setMsg("登录已超时,请重新登录后再尝试!").setCode(101);
		}
		User user = (User) request.getSession().getAttribute("user");
		String img = request.getParameter("img");
		// 调用自定义的工具将base64字符串转成multipartFile,这个multipartFile里的除了响应头和byte数组外的字符(例如文件名,原始文件名)都是随机生成的;
		MultipartFile multipartFile = Base64ToMultipartUtil.base64ToMultipart(img);
		String imgAddr2 = ImgUtil.generateThumbnail(multipartFile, "/user/" + user.getId() + "/", 200, 200);
		if (QiniuStatus.enableQiniu) {
			// 新增的保存到七牛云的云储存部分;
			imgAddr2 = QiniuCloudUtil.upload(PathUtil.getImgBasePath() + imgAddr2,
					imgAddr2.substring(imgAddr2.lastIndexOf("/") + 1));// 注意+1是为了避开/,否则保存的文件名前面会有一个/,
		}
		user.setImgAddr(imgAddr2);
		try {
			userService.save(user);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("保存用户信息时出现异常: 异常信息:"+ e.getMessage());
			}
			return Msg.fail().setMsg("更新头像信息失败");
		}
		return Msg.success().setMsg("修改成功");

	}

	/**
	 * 跳转到注册用户页面
	 * 
	 * @return
	 */
	@RequestMapping(value = "/register", method = RequestMethod.GET)
	private String toRegisterPage(Model model) {
		/**
		 * 取出城市让用户修改居住城市;
		 */
		Pageable pageable = new PageRequest(0, 100, new Sort(Sort.Direction.ASC, "rank"));
		Page<City> cityPage = cityService.findAll(pageable);
		model.addAttribute("cityPage", cityPage);
		return "portal/register";
	}

	/**
	 * ajax检查用户账号是否被占用;
	 * 
	 * @param account
	 * @return
	 */
	@RequestMapping(value = "/user/ajax/checkAccount", method = RequestMethod.POST)
	@ResponseBody
	private Msg ajaxCheckAccount(@RequestParam("inputValue") String account) {
		if (!checkAccount(account)) {
			return Msg.fail().setMsg("账号已被其他人注册!");
		} else {
			return Msg.success().setMsg("账号可以使用!");
		}
	}

	/**
	 * ajax检查用户昵称是否被占用
	 * 
	 * @param nickname
	 * @return
	 */
	@RequestMapping(value = "/user/ajax/checkNickname", method = RequestMethod.POST)
	@ResponseBody
	private Msg ajaxCheckNickname(@RequestParam("inputValue") String nickname) {
		if (!checkNickname(nickname)) {
			return Msg.fail().setMsg("昵称已被他人占用!");
		} else {
			return Msg.success().setMsg("昵称可以使用!");
		}
	}

	/**
	 * 抽取出的检查用户账号是否被占用的方法,false表示因为被占用所以不能使用,true表示可以使用,
	 * 在前端输入框变更时调用一次,提交注册后保存到数据表之前再调用一次;
	 * 
	 * @param account
	 * @return
	 */
	private boolean checkAccount(String account) {
		if (null == account || "".equals(account.trim())) {
			return false;
		}
		User user = new User();
		user.setAccount(account);

		ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id");
		Example<User> userExample = Example.of(user, exampleMatcher);
		Pageable pageable = new PageRequest(0, 2, new Sort(Sort.Direction.DESC, "id"));
		Page<User> allByExample = userService.findAllByExample(userExample, pageable);
		if (allByExample.hasContent()) {// 如果数据库查出的有值,则说明被占用;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 抽取出的检查用户账号是否被占用的方法,false表示因为被占用所以不能使用,true表示可以使用,
	 * 在前端输入框变更时调用一次,提交注册后保存到数据表之前再调用一次,修改用户信息时也会调用一次;
	 * 
	 * @param nickname
	 * @return
	 */
	private boolean checkNickname(String nickname) {
		if (null == nickname || "".equals(nickname.trim())) {
			return false;
		}
		User user = new User();
		user.setNickname(nickname);
		ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("id");
		Example<User> userExample = Example.of(user, exampleMatcher);
		Pageable pageable = new PageRequest(0, 2, new Sort(Sort.Direction.DESC, "id"));
		Page<User> allByExample = userService.findAllByExample(userExample, pageable);
		if (allByExample.hasContent()) {// 如果数据库查出的有值,则说明被占用;
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 用户注册
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/register", method = RequestMethod.POST)
	@ResponseBody
	private Msg saveUser(HttpServletRequest request) {
		// 从前端传来的请求中获取键为userStr的值;
		String userStr = RequestUtil.parserString(request, "userStr");
		System.out.print("userStr的值:" + userStr);
		ObjectMapper objectMapper = new ObjectMapper();
		User user = null;
		try {
			// 将字符串转成实体类
			user = objectMapper.readValue(userStr, User.class);
			if (!checkAccount(user.getAccount())) {
				return Msg.fail().setMsg("账号已经被占用!");
			}
			if (!checkNickname(user.getNickname()) || 16 < user.getNickname().length()) {// 检查昵称重复性并同时限制昵称长度;
				return Msg.fail().setMsg("昵称已经被占用!");
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("读取前端的用户注册信息传值时出现异常: 异常信息:"+ e.getMessage());
			}
			return Msg.fail().setMsg("读取注册信息失败!");
		}
		City city = new City();
		if (null != request.getParameter("cityId")) {
			String cityId = request.getParameter("cityId");
			city.setId(Long.parseLong(cityId));
		} else {
			city.setId(1);// 如果城市参数传递失败则默认选择一个城市,之后管理员审核时可以修改;
		}
		user.setJoinTime(new Date(System.currentTimeMillis()));
		user.setSignature("这个人很懒,没有设置签名...");
		user.setCity(city);
		user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword())); // 使用security推荐的加密方式加密密码;
		user.setImgAddr(Constant.userDefaultHaderImageQiniu);// 先设置用户的默认七牛云端头像,以后等用户修改;
		try {
			userService.save(user);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("保存用户信息时出现异常: 异常信息:"+ e.getMessage());
			}
			return Msg.fail().setMsg("注册失败,保存用户信息时出现错误!");
		}
		// 返回注册店铺的最终结果;
		return Msg.success().setMsg("注册成功!");

	}

	/**
	 * 查看编辑用户信息的页面;
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/user/editInfo", method = RequestMethod.GET)
	public String toUserEditPage(Model model, HttpServletRequest request) {
		/**
		 * 取出城市让用户修改居住城市;
		 */
		Pageable pageable = new PageRequest(0, 100, new Sort(Sort.Direction.ASC, "rank"));
		Page<City> cityPage = cityService.findAll(pageable);
		model.addAttribute("cityPage", cityPage);

		/**
		 * 判断session中是否有user,没有则返回到错误页面或重新登录界面;
		 */
		if (null != request.getSession().getAttribute("user")) {
			User user = (User) request.getSession().getAttribute("user");
			model.addAttribute("user", user);
			return "user/editInfo";
		}
		return "error";
	}

}
