package com.kingguanzhang.toptalk.controller.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingguanzhang.toptalk.dto.Msg;
import com.kingguanzhang.toptalk.entity.Comment;
import com.kingguanzhang.toptalk.entity.Story;
import com.kingguanzhang.toptalk.entity.User;
import com.kingguanzhang.toptalk.repositories.CategoryRepository;
import com.kingguanzhang.toptalk.service.CommentServiceImpl;
import com.kingguanzhang.toptalk.service.StoryServiceImpl;
import com.kingguanzhang.toptalk.utils.ImgUtil;
import com.kingguanzhang.toptalk.utils.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Controller
public class StoryController {

    @Autowired
    private StoryServiceImpl storyService;
    @Autowired
    private CommentServiceImpl commentService;

    /**
     * 获取所有故事,分页并排序;
     * @param model
     * @param pn
     * @return
     */
    @RequestMapping("/story")
    public String toStoryPage(Model model, @RequestParam(value = "pn",defaultValue = "1")Integer pn){
        /**
         * 获取所有故事,分页并排序;
         */
        Pageable pageable = new PageRequest(pn-1,10,new Sort(Sort.Direction.DESC,"id"));
        Page<Story> storyPage = storyService.findAll(pageable);
        model.addAttribute("storyPage",storyPage);

        /**
         * 获取5个最热故事,即收藏数最多的故事;
         */
        Pageable pageable2 = new PageRequest(0,5,new Sort(Sort.Direction.DESC,"collectNumber"));
        Page<Story> hotStoryPage = storyService.findAll(pageable2);
        model.addAttribute("hotStoryPage",hotStoryPage);

        return "/portal/story";
    }


    /**
     * 获取故事详情;
     * @param model
     * @param storyId
     * @return
     */
    @RequestMapping("/story/{storyId}")
    public String toStoryDetailsPage(Model model, @PathVariable("storyId")String storyId,@RequestParam(value = "pn",defaultValue = "1")Integer pn){
        Story story = storyService.findById(Long.parseLong(storyId));
        model.addAttribute("story",story);


        /**
         * 获取topic关联的父Comment,sql语句中已经排除了评论表中supcomment_id 不等于0的情况(即排除掉此评论为子评论时的情况);
         */
        Pageable pageable5 = new PageRequest(pn-1,10,  new Sort(Sort.Direction.DESC,"id"));
        Page<Comment> commentPage = commentService.findByStoryId(Long.parseLong(storyId), pageable5);
        model.addAttribute("commentPage",commentPage);

        return "/portal/storyDetails";

    }

    /**
     * 保存ue富文本编辑器上传的图片并回显;
     * @param upfile
     * @return
     */
    @RequestMapping(value = "/storyContribute/imgUpload")
    @ResponseBody
    public String imgUpload3(MultipartFile upfile) {
        if (upfile.isEmpty()) {
            return "error";
        }

        // TODO 此处user id 需要改成从session中获取security 保存的用户信息来从数据库中查出id:
        User author = new User();
        author.setId(1);
        //设置中间文件夹,方便整理图片
        String centreAddr = "/story/"+author.getId()+"/";
        try {
            //使用工具类保存图片并返回文件名给网页;
            String fileName = ImgUtil.generateThumbnail(upfile,centreAddr,1920,1080);
            //url为文件访问的完整路径,注意应该配合mvc中配置的虚拟路径"/upload"
            String config = "{\"state\": \"SUCCESS\"," +
                    "\"url\": \"" + fileName + "\"," +
                    "\"title\": \"" + fileName + "\"," +
                    "\"original\": \"" + fileName + "\"}";
            return config;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return "error";
    }

    /**
     * 持久化用户投稿
     * @param request
     * @return
     */
    @RequestMapping(value = "/story/contribute",method = RequestMethod.POST)
    @ResponseBody
    private Msg storyContribute(HttpServletRequest request){
        //从前端传来的请求中获取键为shopStr的值;
        String storyStr = RequestUtil.parserString(request, "storyStr");
        System.out.print("storyStr的值是:" + storyStr);
        ObjectMapper objectMapper = new ObjectMapper();
        Story story = null;
        try {
            //将字符串转成实体类
            story = objectMapper.readValue(storyStr, Story.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Msg.fail().setMsg("读取稿件信息失败!");
        }

        // TODO 这里先默认author id为1,后面改成从session中根据登录名获取user id;:
        User author = new User();
        author.setId(1);
        story.setAuthor(author);

        //从request中解析出上传的文件图片;
        MultipartFile coverImg = ((MultipartRequest) request).getFile("img");

        //注册店铺,尽可能的减少从前端获取的值;
        if (null != story && null != coverImg) {
            //设置中间文件夹,方便整理图片
            String centreAddr = "/story/"+author.getId()+"/";
            String imgAddr = ImgUtil.generateThumbnail(coverImg, centreAddr,1920, 1080);
            story.setCommentNumber(134);
            story.setCollectNumber(342);
            story.setCreatTime(new Date(System.currentTimeMillis()));
            story.setCoverImgAddr(imgAddr);

            storyService.save(story);
            //返回注册店铺的最终结果;
            return Msg.success().setMsg("投稿成功,请等待审核.");
        } else {
            return Msg.fail().setMsg("投稿失败,稿件信息不完整!");
        }
    }
}
