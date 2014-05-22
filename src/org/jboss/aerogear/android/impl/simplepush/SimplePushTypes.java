/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jboss.aerogear.android.impl.simplepush;

import org.jboss.aerogear.android.unifiedpush.PushType;

/**
 *
 * @author summers
 */
public enum SimplePushTypes implements PushType {

    TYPE;
    
    @Override
    public String getName() {
        return "SimplePush";
    }
    
}
