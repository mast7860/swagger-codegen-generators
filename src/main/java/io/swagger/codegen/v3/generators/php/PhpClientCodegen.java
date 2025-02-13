package io.swagger.codegen.v3.generators.php;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenSecurity;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public class PhpClientCodegen extends DefaultCodegenConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(PhpClientCodegen.class);

    public static final String VARIABLE_NAMING_CONVENTION = "variableNamingConvention";
    public static final String PACKAGE_PATH = "packagePath";
    public static final String SRC_BASE_PATH = "srcBasePath";
    public static final String COMPOSER_VENDOR_NAME = "composerVendorName";
    public static final String COMPOSER_PROJECT_NAME = "composerProjectName";
    protected String invokerPackage = "Swagger\\Client";
    protected String composerVendorName = null;
    protected String composerProjectName = null;
    protected String packagePath = "SwaggerClient-php";
    protected String artifactVersion = null;
    protected String srcBasePath = "lib";
    protected String testBasePath = "test";
    protected String docsBasePath = "docs";
    protected String apiDirName = "Api";
    protected String modelDirName = "Model";
    protected String variableNamingConvention= "snake_case";
    protected String apiDocPath = docsBasePath + "/" + apiDirName;
    protected String modelDocPath = docsBasePath + "/" + modelDirName;

    public PhpClientCodegen() {
        super();

        // clear import mapping (from default generator) as php does not use it
        // at the moment
        importMapping.clear();

        supportsInheritance = true;
        outputFolder = "generated-code" + File.separator + "php";
        modelTemplateFiles.put("model.mustache", ".php");
        apiTemplateFiles.put("api.mustache", ".php");
        modelTestTemplateFiles.put("model_test.mustache", ".php");
        apiTestTemplateFiles.put("api_test.mustache", ".php");
        embeddedTemplateDir = "php";
        apiPackage = invokerPackage + "\\" + apiDirName;
        modelPackage = invokerPackage + "\\" + modelDirName;

        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        setReservedWordsLowerCase(
                Arrays.asList(
                    // local variables used in api methods (endpoints)
                    "resourcePath", "httpBody", "queryParams", "headerParams",
                    "formParams", "_header_accept", "_tempBody",

                    // PHP reserved words
                    "__halt_compiler", "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "namespace", "new", "or", "print", "private", "protected", "public", "require", "require_once", "return", "static", "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor")
        );

        // ref: http://php.net/manual/en/language.types.intro.php
        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "bool",
                        "boolean",
                        "int",
                        "integer",
                        "double",
                        "float",
                        "string",
                        "object",
                        "DateTime",
                        "mixed",
                        "number",
                        "void",
                        "byte")
        );

        instantiationTypes.put("array", "array");
        instantiationTypes.put("map", "map");


        // provide primitives to mustache template
        List<String> sortedLanguageSpecificPrimitives= new ArrayList<String>(languageSpecificPrimitives);
        Collections.sort(sortedLanguageSpecificPrimitives);
        String primitives = "'" + StringUtils.join(sortedLanguageSpecificPrimitives, "', '") + "'";
        additionalProperties.put("primitives", primitives);

        // ref: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#data-types
        typeMapping = new HashMap<String, String>();
        typeMapping.put("integer", "int");
        typeMapping.put("long", "int");
        typeMapping.put("number", "float");
        typeMapping.put("float", "float");
        typeMapping.put("double", "double");
        typeMapping.put("string", "string");
        typeMapping.put("byte", "int");
        typeMapping.put("boolean", "bool");
        typeMapping.put("date", "\\DateTime");
        typeMapping.put("Date", "\\DateTime");
        typeMapping.put("DateTime", "\\DateTime");
        typeMapping.put("file", "\\SplFileObject");
        typeMapping.put("map", "map");
        typeMapping.put("array", "array");
        typeMapping.put("list", "array");
        typeMapping.put("object", "object");
        typeMapping.put("binary", "string");
        typeMapping.put("ByteArray", "string");
        typeMapping.put("BigDecimal", "float");
        typeMapping.put("UUID", "string");

        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        cliOptions.add(new CliOption(VARIABLE_NAMING_CONVENTION, "naming convention of variable name, e.g. camelCase.")
                .defaultValue("snake_case"));
        cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, "The main namespace to use for all classes. e.g. Yay\\Pets"));
        cliOptions.add(new CliOption(PACKAGE_PATH, "The main package name for classes. e.g. GeneratedPetstore"));
        cliOptions.add(new CliOption(SRC_BASE_PATH, "The directory under packagePath to serve as source root."));
        cliOptions.add(new CliOption(COMPOSER_VENDOR_NAME, "The vendor name used in the composer package name. The template uses {{composerVendorName}}/{{composerProjectName}} for the composer package name. e.g. yaypets. IMPORTANT NOTE (2016/03): composerVendorName will be deprecated and replaced by gitUserId in the next swagger-codegen release"));
        cliOptions.add(new CliOption(CodegenConstants.GIT_USER_ID, CodegenConstants.GIT_USER_ID_DESC));
        cliOptions.add(new CliOption(COMPOSER_PROJECT_NAME, "The project name used in the composer package name. The template uses {{composerVendorName}}/{{composerProjectName}} for the composer package name. e.g. petstore-client. IMPORTANT NOTE (2016/03): composerProjectName will be deprecated and replaced by gitRepoId in the next swagger-codegen release"));
        cliOptions.add(new CliOption(CodegenConstants.GIT_REPO_ID, CodegenConstants.GIT_REPO_ID_DESC));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_VERSION, "The version to use in the composer package version field. e.g. 1.2.3"));
        cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP, "hides the timestamp when files were generated")
                .defaultValue(Boolean.TRUE.toString()));
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String toPackagePath(String packageName, String basePath) {
        return (getPackagePath() + File.separatorChar + toSrcPath(packageName, basePath));
    }

    public String toSrcPath(String packageName, String basePath) {
        packageName = packageName.replace(invokerPackage, ""); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        if (basePath != null && basePath.length() > 0) {
            basePath = basePath.replaceAll("[\\\\/]?$", "") + File.separatorChar; // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        }

        String regFirstPathSeparator;
        if ("/".equals(File.separator)) { // for mac, linux
            regFirstPathSeparator = "^/";
        } else { // for windows
            regFirstPathSeparator = "^\\\\";
        }

        String regLastPathSeparator;
        if ("/".equals(File.separator)) { // for mac, linux
            regLastPathSeparator = "/$";
        } else { // for windows
            regLastPathSeparator = "\\\\$";
        }

        return (basePath
                // Replace period, backslash, forward slash with file separator in package name
                + packageName.replaceAll("[\\.\\\\/]", Matcher.quoteReplacement(File.separator))
                // Trim prefix file separators from package path
                .replaceAll(regFirstPathSeparator, ""))
                // Trim trailing file separators from the overall path
                .replaceAll(regLastPathSeparator+ "$", "");
    }

    @Override
    public String escapeText(String input) {
        if (input != null) {
            // Trim the string to avoid leading and trailing spaces.
            return super.escapeText(input).trim();
        }
        return input;
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "php";
    }

    @Override
    public String getHelp() {
        return "Generates a PHP client library.";
    }

    @Override
    public void processOpts() {
        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));

            // Update the invokerPackage for the default apiPackage and modelPackage
            apiPackage = invokerPackage + "\\" + apiDirName;
            modelPackage = invokerPackage + "\\" + modelDirName;
        } else {
            additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        }

        if (additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            // Update model package to contain the specified model package name and the invoker package
            modelPackage = invokerPackage + "\\" + (String) additionalProperties.get(CodegenConstants.MODEL_PACKAGE);
        }
        additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);

        if (additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            // Update model package to contain the specified model package name and the invoker package
            apiPackage = invokerPackage + "\\" + (String) additionalProperties.get(CodegenConstants.API_PACKAGE);
        }
        additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);

        if (additionalProperties.containsKey(COMPOSER_PROJECT_NAME)) {
            this.setComposerProjectName((String) additionalProperties.get(COMPOSER_PROJECT_NAME));
        } else {
            additionalProperties.put(COMPOSER_PROJECT_NAME, composerProjectName);
        }

        super.processOpts();

        if (additionalProperties.containsKey(PACKAGE_PATH)) {
            this.setPackagePath((String) additionalProperties.get(PACKAGE_PATH));
        } else {
            additionalProperties.put(PACKAGE_PATH, packagePath);
        }

        if (additionalProperties.containsKey(SRC_BASE_PATH)) {
            this.setSrcBasePath((String) additionalProperties.get(SRC_BASE_PATH));
        } else {
            additionalProperties.put(SRC_BASE_PATH, srcBasePath);
        }

        if (additionalProperties.containsKey(CodegenConstants.GIT_USER_ID)) {
            this.setGitUserId((String) additionalProperties.get(CodegenConstants.GIT_USER_ID));
        } else {
            additionalProperties.put(CodegenConstants.GIT_USER_ID, gitUserId);
        }

        if (additionalProperties.containsKey(COMPOSER_VENDOR_NAME)) {
            this.setComposerVendorName((String) additionalProperties.get(COMPOSER_VENDOR_NAME));
        } else {
            additionalProperties.put(COMPOSER_VENDOR_NAME, composerVendorName);
        }

        if (additionalProperties.containsKey(CodegenConstants.GIT_REPO_ID)) {
            this.setGitRepoId((String) additionalProperties.get(CodegenConstants.GIT_REPO_ID));
        } else {
            additionalProperties.put(CodegenConstants.GIT_REPO_ID, gitRepoId);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_VERSION)) {
            this.setArtifactVersion((String) additionalProperties.get(CodegenConstants.ARTIFACT_VERSION));
        } else {
            additionalProperties.put(CodegenConstants.ARTIFACT_VERSION, artifactVersion);
        }

        if (additionalProperties.containsKey(VARIABLE_NAMING_CONVENTION)) {
            this.setParameterNamingConvention((String) additionalProperties.get(VARIABLE_NAMING_CONVENTION));
        }

        additionalProperties.put("escapedInvokerPackage", invokerPackage.replace("\\", "\\\\"));

        // make api and model src path available in mustache template
        additionalProperties.put("apiSrcPath", "./" + toSrcPath(apiPackage, srcBasePath));
        additionalProperties.put("modelSrcPath", "./" + toSrcPath(modelPackage, srcBasePath));
        additionalProperties.put("apiTestPath", "./" + testBasePath + "/" + apiDirName);
        additionalProperties.put("modelTestPath", "./" + testBasePath + "/" + modelDirName);

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        // make test path available in mustache template
        additionalProperties.put("testBasePath", testBasePath);

        supportingFiles.add(new SupportingFile("ApiException.mustache", toPackagePath(invokerPackage, srcBasePath), "ApiException.php"));
        supportingFiles.add(new SupportingFile("Configuration.mustache", toPackagePath(invokerPackage, srcBasePath), "Configuration.php"));
        supportingFiles.add(new SupportingFile("ObjectSerializer.mustache", toPackagePath(invokerPackage, srcBasePath), "ObjectSerializer.php"));
        supportingFiles.add(new SupportingFile("ModelInterface.mustache", toPackagePath(modelPackage, srcBasePath), "ModelInterface.php"));
        supportingFiles.add(new SupportingFile("HeaderSelector.mustache", toPackagePath(invokerPackage, srcBasePath), "HeaderSelector.php"));
        supportingFiles.add(new SupportingFile("composer.mustache", getPackagePath(), "composer.json"));
        supportingFiles.add(new SupportingFile("README.mustache", getPackagePath(), "README.md"));
        supportingFiles.add(new SupportingFile("phpunit.xml.mustache", getPackagePath(), "phpunit.xml.dist"));
        supportingFiles.add(new SupportingFile(".travis.yml", getPackagePath(), ".travis.yml"));
        supportingFiles.add(new SupportingFile(".php_cs", getPackagePath(), ".php_cs"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", getPackagePath(), "git_push.sh"));
    }

    @Override
    public String escapeReservedWord(String name) {
        if(this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return (outputFolder + "/" + toPackagePath(apiPackage, srcBasePath));
    }

    @Override
    public String modelFileFolder() {
        return (outputFolder + "/" + toPackagePath(modelPackage, srcBasePath));
    }

    @Override
    public String apiTestFileFolder() {
        return (outputFolder + "/" + getPackagePath() + "/" + testBasePath + "/" + apiDirName);
    }

    @Override
    public String modelTestFileFolder() {
        return (outputFolder + "/" + getPackagePath() + "/" + testBasePath + "/" + modelDirName);
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + "/" + getPackagePath() + "/" + apiDocPath);
    }

    @Override
    public String modelDocFileFolder() {
        return (outputFolder + "/" + getPackagePath() + "/" + modelDocPath);
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String getTypeDeclaration(Schema propertySchema) {
        if (propertySchema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) propertySchema;
            Schema inner = arraySchema.getItems();
            if (inner == null) {
                LOGGER.warn(arraySchema.getName() + "(array property) does not have a proper inner type defined");
                return "";
            }
            return getTypeDeclaration(inner) + "[]";
        } else if (propertySchema instanceof MapSchema  && hasSchemaProperties(propertySchema)) {
            MapSchema mapSchema = (MapSchema) propertySchema;
            Schema inner = (Schema) mapSchema.getAdditionalProperties();
            if (inner == null) {
                LOGGER.warn(propertySchema.getName() + "(map property) does not have a proper inner type defined");
                return "";
            }
            return getSchemaType(propertySchema) + "[string," + getTypeDeclaration(inner) + "]";
        } else if (propertySchema instanceof MapSchema && hasTrueAdditionalProperties(propertySchema)) {
            Schema inner = new ObjectSchema();
            return getSchemaType(propertySchema) + "[string," + getTypeDeclaration(inner) + "]";
        } else if (StringUtils.isNotBlank(propertySchema.get$ref())) {
            String type = super.getTypeDeclaration(propertySchema);
            return (!languageSpecificPrimitives.contains(type))
                    ? "\\" + modelPackage + "\\" + type : type;
        }

        return super.getTypeDeclaration(propertySchema);
    }

    @Override
    public String getTypeDeclaration(String name) {
        if (!languageSpecificPrimitives.contains(name)) {
            return "\\" + modelPackage + "\\" + name;
        }
        return super.getTypeDeclaration(name);
    }

    @Override
    public String getSchemaType(Schema property) {
        String schemaType = super.getSchemaType(property);
        String type = null;
        if (typeMapping.containsKey(schemaType)) {
            type = typeMapping.get(schemaType);
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            } else if (instantiationTypes.containsKey(type)) {
                return type;
            }
        } else {
            type = schemaType;
        }
        if (type == null) {
            return null;
        }
        return toModelName(type);
    }

    public void setInvokerPackage(String invokerPackage) {
        this.invokerPackage = invokerPackage;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public void setSrcBasePath(String srcBasePath) {
        this.srcBasePath = srcBasePath;
    }

    public void setParameterNamingConvention(String variableNamingConvention) {
        this.variableNamingConvention = variableNamingConvention;
    }

    public void setComposerVendorName(String composerVendorName) {
        this.composerVendorName = composerVendorName;
    }

    public void setComposerProjectName(String composerProjectName) {
        this.composerProjectName = composerProjectName;
    }

    @Override
    protected void processMapSchema(CodegenModel codegenModel, String name, Schema schema) {
        super.processMapSchema(codegenModel, name, schema);
        addVars(codegenModel, schema.getProperties(), schema.getRequired());
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        if ("camelCase".equals(variableNamingConvention)) {
          // return the name in camelCase style
          // phone_number => phoneNumber
          name =  camelize(name, true);
        } else { // default to snake case
          // return the name in underscore style
          // PhoneNumber => phone_number
          name =  underscore(name);
        }

        // parameter name starting with number won't compile
        // need to escape it by appending _ at the beginning
        if (name.matches("^\\d.*")) {
            name = "_" + name;
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        // remove [
        name = name.replaceAll("\\]", "");

        // Note: backslash ("\\") is allowed for e.g. "\\DateTime"
        name = name.replaceAll("[^\\w\\\\]+", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // remove dollar sign
        name = name.replaceAll("$", "");

        // model name cannot use reserved keyword
        if (isReservedWord(name)) {
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        // add prefix and/or suffic only if name does not start wth \ (e.g. \DateTime)
        if (!name.matches("^\\\\.*")) {
            if (!StringUtils.isEmpty(modelNamePrefix)) {
                name = modelNamePrefix + "_" + name;
            }

            if (!StringUtils.isEmpty(modelNameSuffix)) {
                name = name + "_" + modelNameSuffix;
            }
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(name);
    }

    @Override
    public String getArgumentsLocation() {
        return null;
    }

    @Override
    public String getDefaultTemplateDir() {
        return "php";
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String toModelTestFilename(String name) {
        // should be the same as the model name
        return toModelName(name) + "Test";
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + camelize(sanitizeName("call_" + operationId), true));
            operationId = "call_" + operationId;
        }

        return camelize(sanitizeName(operationId), true);
    }

    @Override
    public String toDefaultValue(Schema schema) {
        if (schema instanceof StringSchema) {
            StringSchema stringSchema = (StringSchema) schema;
            if (stringSchema.getDefault() != null) {
                return "'" + stringSchema.getDefault() + "'";
            }
        } else if (schema instanceof BooleanSchema) {
            BooleanSchema booleanSchema = (BooleanSchema) schema;
            if (booleanSchema.getDefault() != null) {
                return booleanSchema.getDefault().toString();
            }
        } else if (schema instanceof DateSchema) {
            // TODO
        } else if (schema instanceof DateTimeSchema) {
            // TODO
        } else if (schema instanceof NumberSchema) {
            NumberSchema numberSchema = (NumberSchema) schema;
            if (
                    numberSchema.getDefault() != null
                    && (SchemaTypeUtil.FLOAT_FORMAT.equals(schema.getFormat()) || SchemaTypeUtil.DOUBLE_FORMAT.equals(schema.getFormat()))
            ) {
                return numberSchema.getDefault().toString();
            }
        } else if (schema instanceof IntegerSchema) {
            IntegerSchema integerSchema = (IntegerSchema) schema;
            if (integerSchema.getDefault() != null) {
                return integerSchema.getDefault().toString();
            }
        }

        return null;
    }

    @Override
    public void setParameterExampleValue(CodegenParameter codegenParameter) {
        String example;

        if (codegenParameter.defaultValue == null) {
            example = codegenParameter.example;
        } else {
            example = codegenParameter.defaultValue;
        }

        String type = codegenParameter.baseType;
        if (type == null) {
            type = codegenParameter.dataType;
        }

        if ("String".equalsIgnoreCase(type)) {
            if (example == null) {
                example = codegenParameter.paramName + "_example";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Integer".equals(type) || "int".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Float".equalsIgnoreCase(type) || "Double".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "3.4";
            }
        } else if ("BOOLEAN".equalsIgnoreCase(type) || "bool".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "True";
            }
        } else if ("\\SplFileObject".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("\\Date".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "2013-10-20";
            }
            example = "new \\DateTime(\"" + escapeText(example) + "\")";
        } else if ("\\DateTime".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "2013-10-20T19:20:30+01:00";
            }
            example = "new \\DateTime(\"" + escapeText(example) + "\")";
        } else if ("object".equals(type)) {
            example = "new \\stdClass";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            if (type != null && type.contains(modelPackage)) {
                example = "new " + type + "()";
            } else {
                example = "new " + getTypeDeclaration(type) + "()";
            }
        } else {
            LOGGER.warn("Type " + type + " not handled properly in setParameterExampleValue");
        }

        boolean isListContainer = getBooleanValue(codegenParameter, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME);
        boolean isMapContainer = getBooleanValue(codegenParameter, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME);

        if (example == null) {
            example = "NULL";
        } else if (isListContainer) {
            example = "array(" + example + ")";
        } else if (isMapContainer) {
            example = "array('key' => " + example + ")";
        }

        codegenParameter.example = example;
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            return value;
        } else {
            return "\'" + escapeText(value) + "\'";
        }
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "_" + value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        if (name.length() == 0) {
            return "EMPTY";
        }

        // number
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            String varName = name;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // for symbol, e.g. $, #
        if (getSymbolName(name) != null) {
            return getSymbolName(name).toUpperCase();
        }

        // string
        String enumName = sanitizeName(underscore(name).toUpperCase());
        enumName = enumName.replaceFirst("^_", "");
        enumName = enumName.replaceFirst("_$", "");

        if (isReservedWord(enumName) || enumName.matches("\\d.*")) { // reserved word or starts with number
            return escapeReservedWord(enumName);
        } else {
            return enumName;
        }
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        String enumName = underscore(toModelName(property.name)).toUpperCase();

        // remove [] for array or map of enum
        enumName = enumName.replace("[]", "");

        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // process enum in models
        return postProcessModelsEnum(objs);
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");
        for (CodegenOperation op : operationList) {
            // for API test method name
            // e.g. public function test{{vendorExtensions.x-testOperationId}}()
            op.vendorExtensions.put("x-testOperationId", camelize(op.operationId));
        }
        return objs;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove ' to avoid code injection
        return input.replace("'", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @Override
    public List<CodegenSecurity> fromSecurity(Map<String, SecurityScheme> securitySchemeMap) {
        final List<CodegenSecurity> securities = super.fromSecurity(securitySchemeMap);
        securities.forEach(codegenSecurity -> {
            final Map<String, Object> extensions = codegenSecurity.getVendorExtensions();
            if (extensions != null && extensions.containsKey("x-token-example")) {
                additionalProperties.put("x-token-example", extensions.get("x-token-example"));
                return;
            }
        });
        return securities;
    }

}
