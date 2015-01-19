package com.gentics.vertx.cailun.core;

import lombok.NoArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.gentics.vertx.cailun.page.PageRepository;

/**
 * Factory which provides link resolvers
 * 
 * @author johannes2
 *
 * @param <T>
 */
@Service
@Scope("singleton")
@NoArgsConstructor
public class CaiLunLinkResolverFactoryImpl<T extends CaiLunLinkResolver> implements LinkResolverFactory<AbstractLinkResolver> {

	@Autowired
	PageRepository pageRepo;

	@Override
	public AbstractLinkResolver createLinkResolver(String link) {
		// TODO replace class with prototype spring DI
		return new CaiLunLinkResolver(link, pageRepo);
	}

}
