package com.flz_chat_business.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("contest_log")
public class ContestLog implements Serializable {
    private Integer id;

    private String operName;//操作名称

    private String operParam;//请求参数

    private String jsonResult;//返回参数

    private String method;//请求方法

    private String requestMethod;//请求方式

    private String operUrl;//请求地址

    private String operUserName;//操作者用户名

    private String operNickName;//操作者昵称

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operTime;//操作时间

    private Long costTime;//消耗时间

    private String operFailMsg;//操作失败信息


    /**
     * 0成功/1失败
     */
    private Integer operResult;//操作结果


}
