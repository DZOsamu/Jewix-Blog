package com.aomsir.jewixapi.service.impl;

import com.aomsir.jewixapi.exception.CustomerException;
import com.aomsir.jewixapi.mapper.TagMapper;
import com.aomsir.jewixapi.pojo.dto.ArticleTagDTO;
import com.aomsir.jewixapi.pojo.entity.Tag;
import com.aomsir.jewixapi.pojo.vo.TagUpdateVo;
import com.aomsir.jewixapi.service.TagService;
import com.aomsir.jewixapi.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

import java.util.Map;


/**
 * @Author: Aomsir
 * @Date: 2023/2/21
 * @Description: 标签业务实现类
 * @Email: info@say521.cn
 * @GitHub: <a href="https://github.com/aomsir">GitHub</a>
 */

@Service
public class TagServiceImpl implements TagService {

    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);
    @Resource
    private TagMapper tagMapper;

    @Override
    public PageUtils searchTagByPage(Map<String, Object> param) {
        Long count = this.tagMapper.searchTagCount();
        ArrayList<Tag> list = null;
        if (count > 0) {
            list = this.tagMapper.queryTagListByPage(param);
        } else {
            list = new ArrayList<>();
        }
        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");
        PageUtils pageUtils = new PageUtils(list,count,start,length);
        return pageUtils;
    }

    @Override
    @Transactional
    public int addTagByName(String tagName) {
        Tag respTag = this.tagMapper.searchTagByName(tagName);
        if (respTag != null) {
            throw new CustomerException("标签已存在");
        }

        Tag tag = new Tag();
        tag.setTagName(tagName);
        tag.setCreateTime(new Date());
        tag.setUpdateTime(new Date());
        int role = this.tagMapper.insertTag(tag);
        log.error("role is {}", role);
        return role;
    }


    @Override
    public int updateTagById(TagUpdateVo updateVo) {
        Tag tag = this.tagMapper.queryTagById(updateVo.getId());
        if (tag == null) {
            throw new CustomerException("暂无此标签");
        }

        // TODO: 修改search为query
        Tag tag_1 = this.tagMapper.searchTagByName(updateVo.getTagName());
        if (tag_1 != null) {
            throw new CustomerException("修改的标签名已存在");
        }

        tag.setTagName(updateVo.getTagName());
        tag.setUpdateTime(new Date());

        int role = this.tagMapper.updateTagById(tag);
        return role;
    }

    @Override
    public Tag searchTagById(Long tagId) {
        Tag tag = this.tagMapper.queryTagById(tagId);
        if (tag == null) {
            throw new CustomerException("标签不存在");
        }
        return tag;
    }

    @Override
    public ArticleTagDTO searchArticleListByTagName(String tagName) {
        Tag tag = this.tagMapper.searchTagByName(tagName);
        if (tag == null) {
            throw new CustomerException("标签不存在");
        }

        ArticleTagDTO articleTagDTO = this.tagMapper.queryArticleListByTagName(tagName);
        return articleTagDTO;
    }
}