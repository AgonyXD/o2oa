package com.x.base.core.project.config;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.x.base.core.project.annotation.FieldDescribe;

public class WebServer extends ConfigObject {

	private static final long serialVersionUID = 7240874589722986538L;

	public static WebServer defaultInstance() {
		return new WebServer();
	}

	public WebServer() {
		this.enable = true;
		this.port = DEFAULT_HTTP_PORT;
		this.sslEnable = false;
		this.proxyHost = "";
		this.proxyPort = null;
		this.statEnable = DEFAULT_STATENABLE;
		this.statExclusions = DEFAULT_STATEXCLUSIONS;
		this.requestLogEnable = DEFAULT_REQUESTLOGENABLE;
		this.requestLogRetainDays = DEFAULT_REQUESTLOGRETAINDAYS;
	}

	private static final Integer DEFAULT_HTTP_PORT = 80;
	private static final Integer DEFAULT_HTTPS_PORT = 443;
	private static final Boolean DEFAULT_STATENABLE = false;
	private static final String DEFAULT_STATEXCLUSIONS = "*.gif,*.jpg,*.png,*.ico";
	private static final Boolean DEFAULT_PROXYCENTERENABLE = true;
	private static final Boolean DEFAULT_PROXYAPPLICATIONENABLE = true;
	private static final Boolean DEFAULT_REQUESTLOGENABLE = false;
	private static final Integer DEFAULT_PROXY_TIMEOUT = 300;
	private static final Integer DEFAULT_REQUESTLOGRETAINDAYS = 7;

	@FieldDescribe("是否启用")
	private Boolean enable;
	@FieldDescribe("http/https端口,用户输入网址后实际访问的第一个端口.http协议默认为80端口,https默认为443端口.")
	private Integer port;
	@FieldDescribe("是否启用ssl传输加密,如果启用将使用config/keystore文件作为密钥文件.使用config/token.json文件中的sslKeyStorePassword字段为密钥密码,sslKeyManagerPassword为管理密码.")
	private Boolean sslEnable;
	@FieldDescribe("代理主机,当服务器是通过apache/nginx等代理服务器映射到公网或者通过路由器做端口映射,在这样的情况下需要设置此地址以标明公网访问地址.")
	private String proxyHost;
	@FieldDescribe("代理端口,当服务器是通过apache/nginx等代理服务器映射到公网或者通过路由器做端口映射,在这样的情况下需要设置此地址以标明公网访问端口.")
	private Integer proxyPort;
	@FieldDescribe("启用统计,默认启用统计.")
	private Boolean statEnable;
	@FieldDescribe("统计忽略路径,默认忽略*.gif,*.jpg,*.png,*.ico")
	private String statExclusions;

	@FieldDescribe("是否启用center服务器代理.")
	private Boolean proxyCenterEnable;

	@FieldDescribe("是否启用application服务器代理")
	private Boolean proxyApplicationEnable;

	@FieldDescribe("代理连接超时时间，默认300(秒)")
	private Integer proxyTimeOut;

	@FieldDescribe("启用访问日志功能.")
	private Boolean requestLogEnable;
	@FieldDescribe("访问日志记录天数,默认7天.")
	private Integer requestLogRetainDays;

	public Boolean getProxyCenterEnable() {
		return proxyCenterEnable == null ? DEFAULT_PROXYCENTERENABLE : this.proxyCenterEnable;
	}

	public Boolean getProxyApplicationEnable() {
		return proxyApplicationEnable == null ? DEFAULT_PROXYAPPLICATIONENABLE : this.proxyApplicationEnable;
	}

	public String getStatExclusions() {
		return (StringUtils.isEmpty(statExclusions) ? DEFAULT_STATEXCLUSIONS : this.statExclusions) + ",/druid/*";
	}

	public Boolean getStatEnable() {
		return BooleanUtils.isNotFalse(statEnable);
	}

	public Boolean getEnable() {
		return BooleanUtils.isTrue(this.enable);
	}

	public Integer getPort() {
		if ((null != this.port) && (this.port > 0) && (this.port < 65535)) {
			return this.port;
		} else {
			if (BooleanUtils.isTrue(this.getSslEnable())) {
				return DEFAULT_HTTPS_PORT;
			} else {
				return DEFAULT_HTTP_PORT;
			}

		}
	}

	public Boolean getSslEnable() {
		return BooleanUtils.isTrue(this.sslEnable);
	}

	public String getProxyHost() {
		return StringUtils.isNotEmpty(this.proxyHost) ? this.proxyHost : "";
	}

	public Integer getProxyPort() {
		if (null != this.proxyPort && this.proxyPort > 0 && this.proxyPort < 65535) {
			return this.proxyPort;
		} else {
			return this.getPort();
		}
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public void setSslEnable(Boolean sslEnable) {
		this.sslEnable = sslEnable;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void setProxyApplicationEnable(Boolean proxyApplicationEnable) {
		this.proxyApplicationEnable = proxyApplicationEnable;
	}

	public void setProxyCenterEnable(Boolean proxyCenterEnable) {
		this.proxyCenterEnable = proxyCenterEnable;
	}

	public Boolean getRequestLogEnable() {
		return BooleanUtils.isTrue(this.requestLogEnable);
	}

	public Integer getProxyTimeOut() {
		return proxyTimeOut == null ? DEFAULT_PROXY_TIMEOUT : this.proxyTimeOut;
	}

	public void setProxyTimeOut(Integer proxyTimeOut) {
		this.proxyTimeOut = proxyTimeOut;
	}

	public Integer getRequestLogRetainDays() {
		return (null == this.requestLogRetainDays || this.requestLogRetainDays < 1) ? DEFAULT_REQUESTLOGRETAINDAYS
				: this.requestLogRetainDays;
	}

}
