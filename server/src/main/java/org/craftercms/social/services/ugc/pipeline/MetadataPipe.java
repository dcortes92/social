package org.craftercms.social.services.ugc.pipeline;

import java.util.Date;
import java.util.Map;

import org.craftercms.profile.api.Profile;
import org.craftercms.social.domain.UGC;
import org.craftercms.social.exceptions.SocialException;
import org.craftercms.social.security.SocialSecurityUtils;
import org.craftercms.social.services.ugc.UgcPipe;

/**
 * Created by cortiz on 5/29/14.
 */
public class MetadataPipe implements UgcPipe {

    @Override
    public <T extends UGC> void process(final T ugc,Map<String,Object> params) throws SocialException {
        if(ugc.getCreatedBy()==null){
            ugc.setCreatedBy(SocialSecurityUtils.getCurrentProfile().getId().toString());
        }
        if (ugc.getCreatedDate() == null) {
            ugc.setCreatedDate(new Date());
        }
        ugc.setLastModifiedDate(new Date());
        if(params!=null && params.containsKey("modifierProfile")){
            Profile profile = (Profile)params.get("modifierProfile");
            ugc.setLastModifiedBy(profile.getId().toString());
        }else {
            ugc.setLastModifiedBy(SocialSecurityUtils.getCurrentProfile().getId().toString());
        }
        ugc.setContextId(SocialSecurityUtils.getContext());
        ugc.setChildren(null);
    }
}
