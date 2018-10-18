package ch.icosys.popjava.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import ch.icosys.popjava.core.base.POPErrorCode;
import ch.icosys.popjava.core.base.POPException;
import ch.icosys.popjava.core.base.POPObject;
import ch.icosys.popjava.core.baseobject.ObjectDescription;
import ch.icosys.popjava.core.baseobject.POPAccessPoint;
import ch.icosys.popjava.core.broker.Broker;
import ch.icosys.popjava.core.buffer.POPBuffer;
import ch.icosys.popjava.core.combox.Combox;
import ch.icosys.popjava.core.system.POPSystem;
import ch.icosys.popjava.core.util.ClassUtil;
import ch.icosys.popjava.core.util.LogWriter;
import ch.icosys.popjava.core.util.SystemUtil;
import ch.icosys.popjava.core.util.Util;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

/**
 * POP-Java Proxy Factory : this class provide methods to create a proxy factory
 * for a specified class. This class uses the Javassit library.
 * 
 *
 */
public class PJProxyFactory extends ProxyFactory {
	/**
	 * Target class to create a proxy
	 */
	protected final Class<?> targetClass;

	/**
	 * Create a new proxy factory for the specified class
	 * 
	 * @param targetClass
	 *            : Class to be created by the Factory
	 */
	public PJProxyFactory(Class<?> targetClass) {
		this.targetClass = targetClass;

		try {
			targetClass.getConstructor();
		} catch (Exception e) {
			throw new POPException(POPErrorCode.UNKNOWN_EXCEPTION,
					"Class " + targetClass.getName() + " does not have a default constructor");
		}

		setSuperclass(targetClass);
		MethodFilter methodFilter = new PJMethodFilter();
		setFilter(methodFilter);
	}

	/**
	 * Create a new object from the factory
	 * 
	 * @param argvs
	 *            : arguments to pass trough the constructor of the specific object
	 * @return the instance of the object
	 * @throws POPException
	 *             a remote exception, look for cause
	 */
	public Object newPOPObject(Broker parentBroker, Object... argvs) throws POPException {
		ObjectDescription objectDescription = POPSystem.getDefaultOD();
		return newPOPObject(parentBroker, objectDescription, argvs);
	}

	/**
	 * All this is because getConstructor does not support Object.class as a
	 * parameter type. Object.class is used by us for null parameters
	 * 
	 * @param targetClass
	 *            the class type
	 * @param parameterTypes
	 *            the constructor parameters
	 * @return the constructor
	 */
	private static Constructor<?> findMatchingConstructor(Class<?> targetClass, Class<?>[] parameterTypes) {
		Constructor<?> constructor = null;

		for (Constructor<?> candidate : targetClass.getConstructors()) {
			if (candidate.getParameterTypes().length == parameterTypes.length) {
				boolean matches = true;

				for (int i = 0; i < parameterTypes.length; i++) {
					if (!parameterTypes[i].isAssignableFrom(candidate.getParameterTypes()[i])
							&& !candidate.getParameterTypes()[i].isAssignableFrom(parameterTypes[i])
							&& !ClassUtil.isAssignableFrom(parameterTypes[i], candidate.getParameterTypes()[i])) {
						matches = false;
					}
				}

				if (matches) {
					if (constructor == null) {
						constructor = candidate;
					} else {
						constructor = null; // Found two matching constructors
						// TODO: Throw exception
						break;
					}
				}
			}
		}

		return constructor;
	}

	/**
	 * Create a new object from specific class and object description.
	 * 
	 * @param od
	 *            : Object description with the resource requirements
	 * @param argvs
	 *            : arguments to pass trough the constructor of the specific object
	 * @return the instance of the object
	 * @throws POPException
	 *             a remote exception, look for cause
	 */
	public Object newPOPObject(Broker parentBroker, ObjectDescription od, Object... argvs) throws POPException {
		try {
			POPObject popObject = null;
			// Check if object has a default constructor

			Class<?>[] parameterTypes = ClassUtil.getObjectTypes(argvs);
			Constructor<?> constructor;

			try {
				constructor = targetClass.getConstructor(parameterTypes);
			} catch (NoSuchMethodException e) {
				constructor = findMatchingConstructor(targetClass, parameterTypes);
			}

			if (constructor == null) {
				System.out.println("No constructor found");
				return null;
			}

			popObject = (POPObject) targetClass.getConstructor().newInstance();
			popObject.loadPOPAnnotations(constructor, argvs);

			ObjectDescription originalOd = popObject.getOd();
			originalOd.merge(od);

			if (originalOd.useLocalJVM()) {
				if (originalOd.getHostName() != null && !originalOd.getHostName().isEmpty()) {
					if (!Util.isLocal(originalOd.getHostName())) {
						throw new POPException(POPErrorCode.METHOD_ANNOTATION_EXCEPTION,
								"Object can't define URL and run in local JVM");
					}
				}

				popObject = (POPObject) constructor.newInstance(argvs);
				popObject.loadPOPAnnotations(constructor, argvs);

				Broker broker = new Broker(popObject);
				SystemUtil.registerLocalJVM(broker);
				return popObject;
			} else {
				if (originalOd.getRemoteAccessPoint() != null && !originalOd.getRemoteAccessPoint().isEmpty()) {
					POPAccessPoint accessPoint = new POPAccessPoint();
					accessPoint.setAccessString(originalOd.getRemoteAccessPoint());
					return bindPOPObject(parentBroker, accessPoint, null);
				}

				PJMethodHandler methodHandler = new PJMethodHandler(parentBroker, popObject);
				methodHandler.setOd(originalOd);
				methodHandler.popConstructor(targetClass, argvs);
				// this.setHandler(methodHandler);
				Class<?> c = createClass();
				Object result = c.newInstance();
				((ProxyObject) result).setHandler(methodHandler);

				return result;
			}
		} catch (POPException e) {
			throw e;
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof POPException) {
				throw (POPException) e.getCause();
			} else {
				LogWriter.writeExceptionLog(e);
			}
		} catch (Exception e) {
			LogWriter.writeExceptionLog(e);
		}

		return null;
	}

	/**
	 * Bind an Interface to her parallel object (her associated Broker)
	 * 
	 * @param accessPoint
	 *            : The accesspoint of the broker
	 * @return ProxyObject which represent the Interface side
	 * @throws POPException
	 *             : if anything goes wrong
	 */
	public Object bindPOPObject(Broker parentBroker, POPAccessPoint accessPoint, String networkUUID)
			throws POPException {
		try {
			Constructor<?> constructor = targetClass.getConstructor();
			POPObject popObject = (POPObject) constructor.newInstance();
			popObject.loadPOPAnnotations(constructor);

			PJMethodHandler methodHandler = new PJMethodHandler(parentBroker, popObject);
			if (networkUUID != null) {
				methodHandler.getOD().setNetwork(networkUUID);
			}

			methodHandler.bindObject(accessPoint);
			// this.setHandler(methodHandler);
			Class<?> c = this.createClass();
			Object result = c.newInstance();
			((ProxyObject) result).setHandler(methodHandler);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new POPException(0, e.getMessage());
		}
	}

	/**
	 * Recover a parallel object from the buffer
	 * 
	 * @param buffer
	 *            : buffer from which the object is recovered
	 * @return the object recovered
	 * @throws POPException
	 *             a remote exception, look for cause
	 */
	public Object newActiveFromBuffer(Combox<?> sourceCombox, POPBuffer buffer) throws POPException {
		POPObject result = null;
		try {
			Constructor<?> constructor = targetClass.getConstructor();
			POPObject popObject = (POPObject) constructor.newInstance();
			popObject.loadPOPAnnotations(constructor);
			PJMethodHandler methodHandler = new PJMethodHandler(null, popObject);
			methodHandler.setSetup();
			// this.setHandler(methodHandler);
			Class<?> c = this.createClass();
			result = (POPObject) c.newInstance();
			((ProxyObject) result).setHandler(methodHandler);

			if (!result.deserialize(sourceCombox, buffer)) {
				LogWriter.writeDebugInfo("bad deserialize");
				POPException.throwObjectBindException(methodHandler.getAccessPoint());
			}
		} catch (Exception e) {
			LogWriter.writeExceptionLog(e);
		}

		LogWriter.writeDebugInfo("result");
		return result;
	}
}
