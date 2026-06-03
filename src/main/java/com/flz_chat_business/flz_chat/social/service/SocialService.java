package com.flz_chat_business.flz_chat.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flz_chat_business.common.enums.ResponseCodeEnum;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.common.util.PageUtils;
import com.flz_chat_business.flz_chat.mapper.FriendshipMapper;
import com.flz_chat_business.flz_chat.mapper.UserInformationMapper;
import com.flz_chat_business.flz_chat.mapper.UserSocialImageMapper;
import com.flz_chat_business.flz_chat.mapper.UserSocialLikeMapper;
import com.flz_chat_business.flz_chat.mapper.UserSocialMapper;
import com.flz_chat_business.flz_chat.pojo.entity.Friendship;
import com.flz_chat_business.flz_chat.pojo.entity.UserInformation;
import com.flz_chat_business.flz_chat.pojo.entity.UserSocial;
import com.flz_chat_business.flz_chat.pojo.entity.UserSocialImage;
import com.flz_chat_business.flz_chat.pojo.entity.UserSocialLike;
import com.flz_chat_business.flz_chat.social.dto.PublishSocialRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 社交动态业务服务。
 */
public class SocialService {

    private final UserSocialMapper userSocialMapper;
    private final UserSocialImageMapper userSocialImageMapper;
    private final UserSocialLikeMapper userSocialLikeMapper;
    private final FriendshipMapper friendshipMapper;
    private final UserInformationMapper userInformationMapper;

    @Transactional(rollbackFor = Exception.class)
    /**
     * 发布动态。
     */
    public Map<String, Object> publish(Long userId, PublishSocialRequest request) {
        UserSocial social = new UserSocial();
        social.setUserId(userId);
        social.setContent(request.getContent());
        social.setVisibility(request.getVisibility() == null ? 0 : request.getVisibility());
        social.setLikeCount(0);
        social.setDeleted(0);
        int imageCount = request.getImages() == null ? 0 : request.getImages().size();
        social.setImageCount(imageCount);
        userSocialMapper.insert(social);

        if (request.getImages() != null) {
            for (PublishSocialRequest.ImageItem image : request.getImages()) {
                UserSocialImage row = new UserSocialImage();
                row.setSocialId(social.getSocialId());
                row.setImageUrl(image.getImageUrl());
                row.setSortOrder(image.getSortOrder() == null ? 0 : image.getSortOrder());
                row.setWidth(image.getWidth());
                row.setHeight(image.getHeight());
                userSocialImageMapper.insert(row);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("socialId", social.getSocialId());
        return result;
    }

    /**
     * 查询用户动态列表。
     */
    public PageData<Map<String, Object>> userSocials(Long currentUserId, Long targetUserId, Integer page, Integer size) {
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        String visibilitySql = resolveVisibilitySql(currentUserId, targetUserId);
        Long total = userSocialMapper.countUserSocials(targetUserId, visibilitySql);
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        List<Map<String, Object>> rows = userSocialMapper.listUserSocials(targetUserId, visibilitySql, PageUtils.offset(p, s), s);
        return new PageData<>(total, p, s, enrichSocialRows(currentUserId, rows));
    }

    /**
     * 查询好友动态流。
     */
    public PageData<Map<String, Object>> feed(Long userId, Integer page, Integer size) {
        int p = PageUtils.normalizePage(page);
        int s = PageUtils.normalizeSize(size);
        List<Friendship> friendships = friendshipMapper.selectList(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getStatus, 1));
        if (friendships.isEmpty()) {
            return PageData.empty(p, s);
        }
        List<Long> friendIds = friendships.stream().map(Friendship::getFriendId).collect(Collectors.toList());
        Long total = userSocialMapper.selectCount(new LambdaQueryWrapper<UserSocial>()
                .in(UserSocial::getUserId, friendIds)
                .eq(UserSocial::getDeleted, 0)
                .in(UserSocial::getVisibility, 0, 1));
        if (total == null || total == 0) {
            return PageData.empty(p, s);
        }
        List<UserSocial> socials = userSocialMapper.selectList(new LambdaQueryWrapper<UserSocial>()
                .in(UserSocial::getUserId, friendIds)
                .eq(UserSocial::getDeleted, 0)
                .in(UserSocial::getVisibility, 0, 1)
                .orderByDesc(UserSocial::getCreatedAt)
                .last("LIMIT " + PageUtils.offset(p, s) + "," + s));
        List<Map<String, Object>> rows = socials.stream().map(this::toBaseSocialMap).collect(Collectors.toList());
        return new PageData<>(total, p, s, enrichSocialRows(userId, rows));
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 删除动态（逻辑删除）。
     */
    public void delete(Long userId, Long socialId) {
        UserSocial social = userSocialMapper.selectById(socialId);
        if (social == null || social.getDeleted() == 1) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "动态不存在");
        }
        if (!userId.equals(social.getUserId())) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN.getCode(), "仅本人可删除动态");
        }
        social.setDeleted(1);
        userSocialMapper.updateById(social);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 点赞动态。
     */
    public void like(Long userId, Long socialId) {
        UserSocial social = userSocialMapper.selectById(socialId);
        if (social == null || social.getDeleted() == 1) {
            throw new BizException(ResponseCodeEnum.NOT_FOUND.getCode(), "动态不存在");
        }
        UserSocialLike exists = userSocialLikeMapper.selectOne(new LambdaQueryWrapper<UserSocialLike>()
                .eq(UserSocialLike::getSocialId, socialId)
                .eq(UserSocialLike::getUserId, userId)
                .last("LIMIT 1"));
        if (exists != null) {
            return;
        }
        UserSocialLike row = new UserSocialLike();
        row.setSocialId(socialId);
        row.setUserId(userId);
        userSocialLikeMapper.insert(row);
        social.setLikeCount((social.getLikeCount() == null ? 0 : social.getLikeCount()) + 1);
        userSocialMapper.updateById(social);
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 取消点赞。
     */
    public void unlike(Long userId, Long socialId) {
        UserSocial social = userSocialMapper.selectById(socialId);
        if (social == null || social.getDeleted() == 1) {
            return;
        }
        UserSocialLike exists = userSocialLikeMapper.selectOne(new LambdaQueryWrapper<UserSocialLike>()
                .eq(UserSocialLike::getSocialId, socialId)
                .eq(UserSocialLike::getUserId, userId)
                .last("LIMIT 1"));
        if (exists == null) {
            return;
        }
        userSocialLikeMapper.deleteById(exists.getId());
        int likeCount = social.getLikeCount() == null ? 0 : social.getLikeCount();
        social.setLikeCount(Math.max(0, likeCount - 1));
        userSocialMapper.updateById(social);
    }

    private String resolveVisibilitySql(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return "0,1,2";
        }
        Friendship friendship = friendshipMapper.selectOne(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, currentUserId)
                .eq(Friendship::getFriendId, targetUserId)
                .eq(Friendship::getStatus, 1)
                .last("LIMIT 1"));
        if (friendship != null) {
            return "0,1";
        }
        return "0";
    }

    private List<Map<String, Object>> enrichSocialRows(Long currentUserId, List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long socialId = toLong(row.get("social_id"), row.get("socialId"));
            Long userId = toLong(row.get("user_id"), row.get("userId"));
            Map<String, Object> item = new HashMap<>();
            item.put("socialId", socialId);
            item.put("userId", userId);
            item.put("content", row.get("content"));
            item.put("visibility", toInt(row.get("visibility")));
            item.put("likeCount", toInt(row.get("like_count"), row.get("likeCount")));
            item.put("createdAt", row.get("created_at") == null ? row.get("createdAt") : row.get("created_at"));

            UserInformation info = userId == null ? null : userInformationMapper.selectById(userId);
            item.put("nickname", info == null ? row.get("nickname") : info.getNickname());
            item.put("avatarUrl", info == null ? row.get("avatar_url") : info.getAvatarUrl());

            List<UserSocialImage> images = socialId == null ? new ArrayList<>() :
                    userSocialImageMapper.selectList(new LambdaQueryWrapper<UserSocialImage>()
                            .eq(UserSocialImage::getSocialId, socialId)
                            .orderByAsc(UserSocialImage::getSortOrder));
            item.put("images", images);

            UserSocialLike liked = socialId == null ? null : userSocialLikeMapper.selectOne(new LambdaQueryWrapper<UserSocialLike>()
                    .eq(UserSocialLike::getSocialId, socialId)
                    .eq(UserSocialLike::getUserId, currentUserId)
                    .last("LIMIT 1"));
            item.put("likedByMe", liked != null);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> toBaseSocialMap(UserSocial social) {
        Map<String, Object> row = new HashMap<>();
        row.put("socialId", social.getSocialId());
        row.put("userId", social.getUserId());
        row.put("content", social.getContent());
        row.put("visibility", social.getVisibility());
        row.put("likeCount", social.getLikeCount());
        row.put("createdAt", social.getCreatedAt());
        return row;
    }

    private Long toLong(Object... values) {
        for (Object value : values) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        }
        return null;
    }

    private Integer toInt(Object... values) {
        for (Object value : values) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return 0;
    }
}
