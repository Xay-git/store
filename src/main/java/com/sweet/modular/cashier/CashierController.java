package com.sweet.modular.cashier;import cn.hutool.json.JSONUtil;import com.sweet.core.exception.ServiceException;import com.sweet.core.model.ResultBean;import com.sweet.core.shiro.ShiroKit;import com.sweet.core.util.BigDecimalUtil;import com.sweet.modular.aop.NoRepeatSubmit;import com.sweet.modular.card.service.CardService;import com.sweet.modular.cashier.cashierconst.CashierConst;import com.sweet.modular.cashier.model.CardForm;import com.sweet.modular.cashier.model.ProductForm;import com.sweet.modular.cashier.service.CashierService;import com.sweet.modular.member.entity.Member;import com.sweet.modular.membercard.entity.Membercard;import com.sweet.modular.membercard.service.MembercardService;import com.sweet.modular.product.entity.Product;import com.sweet.modular.receipt.entity.Receipt;import com.sweet.modular.receipt.service.ReceiptService;import com.sweet.modular.sell.entity.Sell;import com.sweet.modular.sellpay.entity.Sellpay;import com.sweet.modular.starpos.model.BarcodePosPay;import com.sweet.modular.starpos.service.StarPosService;import com.sweet.modular.starpos.util.AmountUtil;import com.sweet.modular.stock.service.StockService;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.stereotype.Controller;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.PostMapping;import org.springframework.web.bind.annotation.RequestMapping;import org.springframework.web.bind.annotation.ResponseBody;import java.io.UnsupportedEncodingException;import java.math.BigDecimal;import java.net.URLDecoder;import java.rmi.ServerException;import java.util.ArrayList;import java.util.List;import java.util.Map;@RequestMapping("/admin/cashier")@Controllerpublic class CashierController {    @Autowired    CashierService cashierService;    @Autowired    MembercardService membercardService;    @Autowired    StockService stockService;    @Autowired    StarPosService starPosService;    @Autowired    ReceiptService receiptService;    @RequestMapping("")    public String cashier(){        return "/modular/cashier/cashier";    }    @RequestMapping("/member_cashier")    public String member_cashier(){        return "/modular/cashier/member_cashier";    }    @RequestMapping("/people_cashier")    public String people_cashier(){        return "/modular/cashier/people_cashier";    }    @RequestMapping("/member_add")    public String member_add(){        return "/modular/cashier/member_add";    }    @RequestMapping("/member_edit")    public String member_edit(){        return "/modular/cashier/member_edit";    }    @PostMapping("/buildCard")//    @NoRepeatSubmit    @ResponseBody    @Transactional    public ResultBean buildCard(String user,String card,String pay){        Member member = JSONUtil.toBean(user,Member.class);        CardForm cardForm = JSONUtil.toBean(card, CardForm.class);        List<Sellpay> sellpayList = JSONUtil.toList(JSONUtil.parseArray(pay), Sellpay.class);        BigDecimal sellAmount = cardForm.getAmount();        //会员开卡        Membercard membercard = cashierService.buildMemberCard(member,cardForm);        //创建销售总单        Sell sell = cashierService.addSell(member,sellAmount,sellpayList,CashierConst.SELL_BUILD_CARD);        cardForm.setMemberCardId(membercard.getId());        cardForm.setRealAmount(sell.getRealAmount());        //创建销售细单        cashierService.addSellDetail(sell.getId(),sellAmount,CashierConst.SELL_BUILD_CARD,cardForm);        //创建付款方式明细        cashierService.addSellPay(sell.getId(),sellpayList);        return ResultBean.success(member);    }    @PostMapping("/chargedCard")//    @NoRepeatSubmit    @ResponseBody    @Transactional    public ResultBean chargedCard(String user,String card,String pay){        Member member = JSONUtil.toBean(user,Member.class);        CardForm cardForm = JSONUtil.toBean(card, CardForm.class);        List<Sellpay> sellpayList = JSONUtil.toList(JSONUtil.parseArray(pay), Sellpay.class);        BigDecimal sellAmount = cardForm.getAmount();        //会员充值        Membercard membercard = cashierService.chargedMemberCard(member,cardForm);        //创建销售总单        Sell sell = cashierService.addSell(member,sellAmount,sellpayList,CashierConst.SELL_CHARGED_CARD);        cardForm.setRealAmount(sell.getRealAmount());        cardForm.setMemberCardId(membercard.getId());        //创建销售细单        cashierService.addSellDetail(sell.getId(),sellAmount,CashierConst.SELL_CHARGED_CARD,cardForm);        //创建付款方式明细        cashierService.addSellPay(sell.getId(),sellpayList);        return ResultBean.success(member);    }    @PostMapping("/productSubmit")//    @NoRepeatSubmit    @ResponseBody    @Transactional    public ResultBean productSubmit(String user,String productFormList,String pay,BigDecimal sellAmount,String memberCard,String receiptId ){        Member member = JSONUtil.toBean(user,Member.class);        List<ProductForm> productForms = JSONUtil.toList(JSONUtil.parseArray(productFormList), ProductForm.class);        List<Sellpay> sellpayList = JSONUtil.toList(JSONUtil.parseArray(pay), Sellpay.class);        Membercard card = JSONUtil.toBean(memberCard,Membercard.class);        //会员卡扣款        if(card.getId()!=null){            cashierService.deductMemberCard(card,sellpayList);        }        //创建销售总单        Sell sell = cashierService.addSell(member,sellAmount,sellpayList,CashierConst.SELL_PRODUCT);        //创建条码        Receipt receipt = receiptService.getById(receiptId);        receipt.setSellId(sell.getId());        receiptService.updateById(receipt);        //创建卖品集合        List<ProductForm> products = new ArrayList<>();        productForms.stream().forEach(productForm -> {            if(productForm.getProductType()==2){                products.add(productForm);            }            productForm.setMemberCardId(card.getId());            //创建销售细单            cashierService.addSellDetail(sell.getId(),sellAmount,CashierConst.SELL_PRODUCT,productForm);        });        //消耗库存        stockService.consumptionStock(products);        //创建付款方式明细        cashierService.addSellPay(sell.getId(),sellpayList);        return ResultBean.success(member);    }    @RequestMapping("/pay")    @ResponseBody    public ResultBean pay(String amount, String authCode, String payChannel) throws UnsupportedEncodingException {//        String realAmount = AmountUtil.changeY2F(amount);//        System.out.println("支付的金额为"+realAmount+"分");        BarcodePosPay barcodePosPay = starPosService.buildPay("1",authCode,payChannel);        Map map = starPosService.excutePay(barcodePosPay);        //添加条码支付记录        Receipt receipt = new Receipt();        receipt.setAmount(barcodePosPay.getAmount());        receipt.setCollStatus(0);        receipt.setTradeNo(barcodePosPay.getTradeNo());        receipt.setTrmNo(barcodePosPay.getTrmNo());        receiptService.save(receipt);        String result = String.valueOf(map.get("result"));        String msg = URLDecoder.decode((String) map.get("message"),"utf-8");        if(!result.equals("S")){            throw new ServiceException(msg);        }else{        }        return ResultBean.success(receipt.getId());    }    @RequestMapping("/refundBarcodePay")    @ResponseBody    public ResultBean refundBarcodePay(String orderNo) throws UnsupportedEncodingException {        Map map = starPosService.refundBarcodePay(orderNo);        return ResultBean.success(map);    }}