package com.aomsir.jewixapi.pojo.entity;


<<<<<<< HEAD
import java.io.Serializable;
=======
import lombok.Data;

>>>>>>> dev
import java.util.Date;


/**
 * @Author: Aomsir
 * @Date: 2023/2/19
 * @Description: 通用实体类
 * @Email: info@say521.cn
 * @GitHub: <a href="https://github.com/aomsir">GitHub</a>
 */

@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    public Date createTime;
    public Date updateTime;
    public Integer status;

}
