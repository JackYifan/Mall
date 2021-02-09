package com.atguigu.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class DoubleValueConstrainValidator implements ConstraintValidator<ListValueOfDouble,Double> {
    //存储所有的合法值
    Set<Double>set = new HashSet<>();

    /**
     * 注解的初始化操作，在判断之前
     * @param constraintAnnotation
     */
    @Override
    public void initialize(ListValueOfDouble constraintAnnotation) {
        //注解的属性中vals中的值
        double[] vals = constraintAnnotation.vals();
        for(double val:vals){
            set.add(val);
        }
    }

    /**
     * 判断是否合法
     * @param aDouble 注解标注的属性的值
     * @param constraintValidatorContext
     * @return
     */
    @Override
    public boolean isValid(Double aDouble, ConstraintValidatorContext constraintValidatorContext) {
        return set.contains(aDouble);
    }
}
