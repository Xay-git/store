package com.sweet.modular.cashier.service.impl;import com.sweet.core.exception.ServiceException;import com.sweet.core.util.BigDecimalUtil;import com.sweet.modular.card.entity.Card;import com.sweet.modular.cashier.cashierconst.CashierConst;import com.sweet.modular.cashier.model.CardForm;import com.sweet.modular.cashier.model.CashierForm;import com.sweet.modular.cashier.model.ProductForm;import com.sweet.modular.cashier.service.CashierService;import com.sweet.modular.member.entity.Member;import com.sweet.modular.membercard.entity.Membercard;import com.sweet.modular.membercard.service.MembercardService;import com.sweet.modular.sell.entity.Sell;import com.sweet.modular.selldetail.entity.Selldetail;import com.sweet.modular.sellpay.entity.Sellpay;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.stereotype.Service;import java.math.BigDecimal;import java.util.List;@Servicepublic class CashierServiceImpl implements CashierService {    @Autowired    MembercardService membercardService;    @Override    public Sell addSell(Member member, BigDecimal amount, List<Sellpay> sellpayList, Integer SellType) {        //优惠金额        Sellpay dicountPay = sellpayList.stream().filter(p->p.getPayType()== CashierConst.PAY_DISCOUNT).findFirst().orElse(null);        Sell sell = new Sell();        sell.setMemberId(member.getId());        sell.setMemberName(member.getName());        sell.setSellAmount(amount);        //使用了优惠        if(dicountPay!=null){            sell.setRealAmount(BigDecimalUtil.sub(sell.getSellAmount(),dicountPay.getAmount()));        }else{            sell.setRealAmount(sell.getSellAmount());        }        sell.setSellType(SellType);        sell.setStatus(CashierConst.SELL_STATUS_NORMAL);        sell.insert();        return sell;    }    @Override    public Selldetail addSellDetail(Long sellId,BigDecimal amount, Integer SellType, CashierForm sellForm) {        Selldetail selldetail = new Selldetail();        selldetail.setSellId(sellId);        selldetail.setName(sellForm.getName());        selldetail.setMemberCardId(sellForm.getMemberCardId());        Long memberCardId = selldetail.getMemberCardId();        Membercard membercard = membercardService.getById(memberCardId);        switch(SellType){            case CashierConst.SELL_BUILD_CARD:                selldetail.setAmount(amount);                selldetail.setGiveAmount(((CardForm) sellForm).getGiveAmount());                BigDecimal totalChardAmount = BigDecimalUtil.add(selldetail.getAmount(),selldetail.getGiveAmount());                selldetail.setMemberCardAmount(totalChardAmount);                selldetail.setCardId(sellForm.getId());                selldetail.setRealAmount(((CardForm) sellForm).getRealAmount());                break;            case CashierConst.SELL_CHARGED_CARD:                selldetail.setAmount(amount);                if(selldetail.getGiveAmount()==null){                    selldetail.setGiveAmount(new BigDecimal(0.00));                }else{                    selldetail.setGiveAmount(((CardForm) sellForm).getGiveAmount());                }                if(membercard!=null){                    selldetail.setMemberCardAmount(membercard.getAmount());                }                selldetail.setRealAmount(((CardForm) sellForm).getRealAmount());                break;            case CashierConst.SELL_PRODUCT:                selldetail.setProductId(sellForm.getId());                selldetail.setAmount(((ProductForm) sellForm).getAmount());                selldetail.setRealAmount(((ProductForm) sellForm).getRealAmount());                selldetail.setDiscout(((ProductForm) sellForm).getDiscount());                selldetail.setPushMoney(((ProductForm) sellForm).getPushMoney());                if(membercard!=null){                    selldetail.setMemberCardAmount(membercard.getAmount());                }                break;            default:                break;        }        selldetail.setSellType(SellType);        selldetail.setTechId(sellForm.getTechId());        selldetail.setTechName(sellForm.getTechName());        selldetail.insert();        return selldetail;    }    @Override    public Sellpay addSellPay(Long sellId, List<Sellpay> sellpayList) {        sellpayList.stream().forEach(pay -> {            pay.setSellId(sellId);            pay.insert();        });        return null;    }    @Override    public Membercard buildMemberCard(Member member, CardForm sellForm) {        Membercard membercard = new Membercard();        membercard.setMemberId(member.getId());        membercard.setName(sellForm.getName());        membercard.setAmount(BigDecimalUtil.add(sellForm.getAmount(),sellForm.getGiveAmount()));        membercard.setProjectDiscount(sellForm.getProjectDiscount());        membercard.setGoodDiscount(sellForm.getGoodDiscount());        membercard.insert();        return membercard;    }    @Override    public Membercard chargedMemberCard(Member member, CardForm sellForm) {        sellForm = (CardForm)sellForm;        Membercard membercard = membercardService.getById(sellForm.getId());        //计算原有余额与充值余额总和        BigDecimal cardAmount = membercard.getAmount();        BigDecimal chargedAmount = sellForm.getAmount();        BigDecimal nowAmount = BigDecimalUtil.add(cardAmount,chargedAmount);        membercard.setAmount(nowAmount);        membercardService.updateById(membercard);        return membercard;    }    @Override    public Membercard deductMemberCard(Membercard card,List<Sellpay> sellpayList) {        //会员卡扣款        card = membercardService.getById(card.getId());        //会员卡扣款        if(card==null){            throw new ServiceException("会员卡不存在~");        }        Sellpay memberCardPay = sellpayList.stream().filter(p->p.getPayType()== CashierConst.PAY_MEMBERCARD).findFirst().orElse(null);        BigDecimal cardAmount = card.getAmount();        if(memberCardPay!=null){            if(memberCardPay.getAmount().compareTo(cardAmount)==1){                throw new ServiceException("会员卡余额不足，结算失败~");            }            BigDecimal cardBalance = BigDecimalUtil.sub(cardAmount,memberCardPay.getAmount());            card.setAmount(cardBalance);            membercardService.updateById(card);        }        return card;    }}