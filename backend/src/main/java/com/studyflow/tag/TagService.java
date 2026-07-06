package com.studyflow.tag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.tag.dto.TagRequest;
import com.studyflow.tag.dto.TagResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {
    private final TagMapper tagMapper;

    public TagService(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Transactional
    public TagResponse createTag(Long userId, TagRequest request) {
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getName, request.name()));
        if (count > 0) {
            throw new BusinessException(400, "标签已存在");
        }

        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(request.name());
        tag.setColor(normalizeColor(request.color()));
        tagMapper.insert(tag);
        return TagResponse.from(tag);
    }

    public List<TagResponse> listTags(Long userId) {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getUserId, userId)
                        .orderByDesc(Tag::getId))
                .stream()
                .map(TagResponse::from)
                .toList();
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return "#1677ff";
        }
        return color;
    }
}
