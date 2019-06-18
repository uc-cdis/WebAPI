package org.ohdsi.webapi.shiro.runas;

import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultInMemoryRunAsStorage implements RunAsStorage {

  private final Map<Object, List<PrincipalCollection>> principalsMap = new ConcurrentHashMap<>();
  private final Logger logger = LoggerFactory.getLogger(DefaultInMemoryRunAsStorage.class);
  @Override
  public void pushPrincipals(Object principal, PrincipalCollection principals) {

    if (Objects.isNull(principals) || principals.isEmpty()) {
      throw new IllegalArgumentException("Specified Subject principals cannot be null or empty for 'run as' functionality.");
    }
    List<PrincipalCollection> stack = getRunAsPrincipalStack(principal);
    if (Objects.isNull(stack)) {
      stack = new CopyOnWriteArrayList<>();
      principalsMap.put(principal, stack);
      logger.error("put stack to principalsMap with principal: {}", principal);
    }
    stack.add(0, principals);
  }

  @Override
  public PrincipalCollection popPrincipals(Object principal) {

    PrincipalCollection popped = null;

    List<PrincipalCollection> stack = getRunAsPrincipalStack(principal);
    logger.error("popPrincipals size: {}", stack == null ? 0 : stack.size());
    if (!Objects.isNull(stack) && !stack.isEmpty()) {
      popped = stack.remove(0);
      if (stack.isEmpty()) {
      logger.error("popPrincipals, stack is empty removeRunAsStack");
        removeRunAsStack(principal);
      }
    }

    return popped;
  }

  @Override
  public List<PrincipalCollection> getRunAsPrincipalStack(Object principal) {

    if (Objects.isNull(principal)) {
      throw new IllegalArgumentException("Token should not be null value");
    }
    List<PrincipalCollection> principalCollections = principalsMap.get(principal);
    logger.error("getRunAsPrincipalStack: {}, size: {}", principal, principalCollections == null ? 0 : principalCollections.size());
    return principalCollections;
  }

  @Override
  public void removeRunAsStack(Object principal) {
    logger.error("removeRunAsStack: {}", principal);
    principalsMap.remove(principal);
  }
}
