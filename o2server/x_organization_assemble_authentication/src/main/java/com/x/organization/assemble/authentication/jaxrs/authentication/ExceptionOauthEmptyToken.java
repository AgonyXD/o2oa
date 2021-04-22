package com.x.organization.assemble.authentication.jaxrs.authentication;

import com.x.base.core.project.exception.LanguagePromptException;

class ExceptionOauthEmptyToken extends LanguagePromptException {

	private static final long serialVersionUID = 4132300948670472899L;

	public static String defaultMessage = "请求令牌返回值为空.";

	ExceptionOauthEmptyToken() {
		super(defaultMessage);
	}
}
