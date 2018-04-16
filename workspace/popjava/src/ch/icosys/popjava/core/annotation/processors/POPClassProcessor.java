package ch.icosys.popjava.core.annotation.processors;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;

import ch.icosys.popjava.core.annotation.POPAsyncConc;
import ch.icosys.popjava.core.annotation.POPAsyncMutex;
import ch.icosys.popjava.core.annotation.POPAsyncSeq;
import ch.icosys.popjava.core.annotation.POPClass;
import ch.icosys.popjava.core.annotation.POPPrivate;
import ch.icosys.popjava.core.annotation.POPSyncConc;
import ch.icosys.popjava.core.annotation.POPSyncMutex;
import ch.icosys.popjava.core.annotation.POPSyncSeq;

/**
 * http://www.javaspecialists.eu/archive/Issue167.html
 */
@SupportedAnnotationTypes("popjava.annotation.POPClass")
//@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class POPClassProcessor extends AbstractProcessor {

	private Messager messager;

	@Override
	public void init(ProcessingEnvironment env) {
		messager = env.getMessager();
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
		for (TypeElement type : annotations) {
			processNoArgsConstructorClasses(env, type);
		}
		
		return true;
	}

	private void processNoArgsConstructorClasses(RoundEnvironment env, TypeElement type) {
		for (Element element : env.getElementsAnnotatedWith(type)) {
			processClass(element);
		}
	}

	private void processClass(Element element) {
	    POPClass popAnnotation = element.getAnnotation(POPClass.class);
	    if(popAnnotation == null){
	        messager.printMessage(Diagnostic.Kind.ERROR, "POP-Class " + element + " did not have the @"+POPClass.class.getName()+" annotation", element);
	        return;
	    }
	    
	    if(!popAnnotation.isDistributable()){
	        return;
	    }
	    
	    //Check if the POPClass defines a constructor with no arguments
		if (!doesClassContainNoArgsConstructor(element)) {
			messager.printMessage(Diagnostic.Kind.ERROR, "Class " + element + " needs a No-Args Constructor", element);
		}
		
		//Check all public methods for conformity
		for (Element subelement : element.getEnclosedElements()) {
            if(subelement.getKind() == ElementKind.METHOD && subelement.getModifiers().contains(Modifier.PUBLIC)){
                checkPublicMethodAnnotation(subelement);
                //TODO: do a basic check if all parameters of the method can be serialized
                /*if(!areParametersSerializable(subelement)){
                    messager.printMessage(Diagnostic.Kind.ERROR, "Can not serialize all parameters of "+subelement, subelement);
                }*/
            }
        }
	}
	
	private void checkPublicMethodAnnotation(Element method){
	    if(method.getModifiers().contains(Modifier.STATIC)){
	        messager.printMessage(Diagnostic.Kind.ERROR, "Method " + method + " can not be public and static", method);
	    }
	    
		if(method.getAnnotation(POPAsyncSeq.class) == null &&
				method.getAnnotation(POPAsyncConc.class) == null &&
				method.getAnnotation(POPAsyncMutex.class) == null &&
				method.getAnnotation(POPSyncSeq.class) == null &&
				method.getAnnotation(POPSyncConc.class) == null &&
				method.getAnnotation(POPSyncMutex.class) == null &&
				method.getAnnotation(POPPrivate.class) == null){
			messager.printMessage(Diagnostic.Kind.WARNING,
					"Method " + method + " is public but does not have a POP-Java annotation, @"+POPSyncConc.class.getSimpleName()+" will be used",
					method);
			return;
		}
		
		MethodReturnType types = getMethodData(method);
		
		//Test if non void methods are synchronous
		if(types.types.getKind() != TypeKind.VOID){		
			if(method.getAnnotation(POPAsyncSeq.class) != null ||
					method.getAnnotation(POPAsyncConc.class) != null ||
					method.getAnnotation(POPAsyncMutex.class) != null){
				messager.printMessage(Diagnostic.Kind.ERROR,
						"Method " + method + " is of non void return type but is asynchronous");
			}
		}
	}

	private MethodReturnType getMethodData(final Element methodElement) {
		MethodReturnType data = new MethodReturnType();

        TypeMirror mirror = methodElement.asType();
        mirror.accept(visitor(data), null);

        return data;
	}
	
	public boolean areParametersSerializable(Element el){
	    TypeMirror mirror = el.asType();
        if (mirror.accept(SERIALIZABLE_ARGS_VISITOR, null)){
            return true;
        }
        
        return false;
	}
	
	private boolean doesClassContainNoArgsConstructor(Element el) {
		for (Element subelement : el.getEnclosedElements()) {
			if (subelement.getKind() == ElementKind.CONSTRUCTOR && subelement.getModifiers().contains(Modifier.PUBLIC)) {
				TypeMirror mirror = subelement.asType();
				if (mirror.accept(NO_ARGS_VISITOR, null)){
					return true;
				}
			}
		}
		return false;
	}
	
	private TypeVisitor<Boolean, Void> visitor(final MethodReturnType data) {

        return new SimpleTypeVisitor6<Boolean, Void>() {

                @Override
                public Boolean visitExecutable(ExecutableType t, Void v) {
                	data.types = t.getReturnType();
                    return true;
                }
        };
	}
	
	private static final TypeVisitor<Boolean, Void> SERIALIZABLE_ARGS_VISITOR = new SimpleTypeVisitor6<Boolean, Void>() {
        
	    @Override
        public Boolean visitExecutable(ExecutableType t, Void v) {
            for(TypeMirror type: t.getParameterTypes()){
                if(!type.getKind().isPrimitive()){
                    return false;
                }
                
                //Sadly it is not possible to get the class of the parameter due to the way annotation preprocessors work
            }
            return true;
        }
    };

	private static final TypeVisitor<Boolean, Void> NO_ARGS_VISITOR = new SimpleTypeVisitor6<Boolean, Void>() {
		@Override
        public Boolean visitExecutable(ExecutableType t, Void v) {
			return t.getParameterTypes().isEmpty();
		}
	};
	
	private class MethodReturnType{
		private TypeMirror types;
	}
	
	@Override
    public SourceVersion getSupportedSourceVersion() {
	    return SourceVersion.latestSupported();	    
	}
}
