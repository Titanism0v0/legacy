package com.overseas.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exchange_rate")
public class ExchangeRate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String currency;
    private BigDecimal rateToCny;
    private String source;
    private LocalDate quoteDate;
    private LocalDateTime fetchTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
