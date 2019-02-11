package com.kakacl.product_service.service;

import com.github.pagehelper.PageInfo;
import com.kakacl.product_service.domain.Account;

import java.util.List;
import java.util.Map;

public interface AccountService {

    Map selectHistoryByYserId(Map params);

    Map selectById(java.util.Map params);

    Map selectByPhone(java.util.Map params);

    List<Map> selectByPhoneORKakaNum(java.util.Map params);

    PageInfo<Map> selectByPageAndSelections(int currentPage, int pageSize);

    /*
     * 更新个人资料
     *
     * @author wangwei
     * @date 2019/2/10
      * @param params
     * @return boolean
     */
    boolean updateInfo(Map params);

    /*
     * 更新个人头像地址
     *
     * @author wangwei
     * @date 2019/2/10
     * @param params
     * @return boolean
     */
    boolean updateHead(Map params);
}
