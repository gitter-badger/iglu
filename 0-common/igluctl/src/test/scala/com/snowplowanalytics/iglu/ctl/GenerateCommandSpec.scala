/*
 * Copyright (c) 2012-2016 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.iglu.ctl

import org.specs2.Specification
import org.json4s.jackson.JsonMethods.parse
import java.io.File

import com.snowplowanalytics.iglu.ctl.GenerateCommand.{DdlOutput, Errors, VersionSuccess, Warnings}
import com.snowplowanalytics.iglu.ctl.FileUtils.TextFile
import com.snowplowanalytics.iglu.ctl.Utils.splitValidations
import com.snowplowanalytics.iglu.ctl.FileUtils.JsonFile

// Scalaz
import scalaz._
import Scalaz._


class GenerateCommandSpec extends Specification { def is = s2"""
  DDL-generation command (ddl) specification
    correctly convert com.amazon.aws.lambda/java_context_1 with default arguments $e1
    correctly convert com.amazon.aws.lambda/java_context_1 with --raw --no-header --varchar 128 $e2
    correctly convert com.amazon.aws.ec2/instance_identity_1 with --no-header --schema snowplow $e3
    correctly produce JSONPaths file for com.amazon.aws.cloudfront/wd_access_log_1 $e4
    output correct warnings for DDL-generation process $e5
    warn about missing schema versions (addition) $e6
    warn about missing 1-0-0 schema version $e7
    correctly setup table ownership $e8
  """

  def e1 = {
    val resultContent =
      """|CREATE SCHEMA IF NOT EXISTS atomic;
         |
         |CREATE TABLE IF NOT EXISTS atomic.com_amazon_aws_lambda_java_context_1 (
         |    "schema_vendor"                          VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "schema_name"                            VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "schema_format"                          VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "schema_version"                         VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "root_id"                                CHAR(36)      ENCODE RAW  NOT NULL,
         |    "root_tstamp"                            TIMESTAMP     ENCODE ZSTD NOT NULL,
         |    "ref_root"                               VARCHAR(255)  ENCODE ZSTD NOT NULL,
         |    "ref_tree"                               VARCHAR(1500) ENCODE ZSTD NOT NULL,
         |    "ref_parent"                             VARCHAR(255)  ENCODE ZSTD NOT NULL,
         |    "aws_request_id"                         VARCHAR(4096) ENCODE ZSTD,
         |    "client_context.client.app_package_name" VARCHAR(4096) ENCODE ZSTD,
         |    "client_context.client.app_title"        VARCHAR(4096) ENCODE ZSTD,
         |    "client_context.client.app_version_code" VARCHAR(4096) ENCODE ZSTD,
         |    "client_context.client.app_version_name" VARCHAR(4096) ENCODE ZSTD,
         |    "client_context.custom"                  VARCHAR(4096) ENCODE ZSTD,
         |    "client_context.environment"             VARCHAR(4096) ENCODE ZSTD,
         |    "function_name"                          VARCHAR(4096) ENCODE ZSTD,
         |    "identity.identity_id"                   VARCHAR(4096) ENCODE ZSTD,
         |    "identity.identity_pool_id"              VARCHAR(4096) ENCODE ZSTD,
         |    "log_group_name"                         VARCHAR(4096) ENCODE ZSTD,
         |    "log_stream_name"                        VARCHAR(4096) ENCODE ZSTD,
         |    "memory_limit_in_mb"                     BIGINT        ENCODE ZSTD,
         |    "remaining_time_millis"                  BIGINT        ENCODE ZSTD,
         |    FOREIGN KEY (root_id) REFERENCES atomic.events(event_id)
         |)
         |DISTSTYLE KEY
         |DISTKEY (root_id)
         |SORTKEY (root_tstamp);
         |
         |COMMENT ON TABLE atomic.com_amazon_aws_lambda_java_context_1 IS 'iglu:com.amazon.aws.lambda/java_context/jsonschema/1-0-0';""".stripMargin

    val sourceSchema = parse(
      """
        |{
        |	"$schema":"http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
        |	"description":"Schema for an AWS Lambda Java context object, http://docs.aws.amazon.com/lambda/latest/dg/java-context-object.html",
        |	"self":{
        |		"vendor":"com.amazon.aws.lambda",
        |		"name":"java_context",
        |		"version":"1-0-0",
        |		"format":"jsonschema"
        |	},
        |	"type":"object",
        |	"properties":{
        |		"functionName":{
        |			"type":"string"
        |		},
        |		"logStreamName":{
        |			"type":"string"
        |		},
        |		"awsRequestId":{
        |			"type":"string"
        |		},
        |		"remainingTimeMillis":{
        |			"type":"integer",
        |			"minimum":0
        |		},
        |		"logGroupName":{
        |			"type":"string"
        |		},
        |		"memoryLimitInMB":{
        |			"type":"integer",
        |			"minimum":0
        |		},
        |		"clientContext":{
        |			"type":"object",
        |			"properties":{
        |				"client":{
        |					"type":"object",
        |					"properties":{
        |						"appTitle":{
        |							"type":"string"
        |						},
        |						"appVersionName":{
        |							"type":"string"
        |						},
        |						"appVersionCode":{
        |							"type":"string"
        |						},
        |						"appPackageName":{
        |							"type":"string"
        |						}
        |					},
        |					"additionalProperties":false
        |				},
        |				"custom":{
        |					"type":"object",
        |					"patternProperties":{
        |						".*":{
        |							"type":"string"
        |						}
        |					}
        |				},
        |				"environment":{
        |					"type":"object",
        |					"patternProperties":{
        |						".*":{
        |							"type":"string"
        |						}
        |					}
        |				}
        |			},
        |			"additionalProperties":false
        |		},
        |		"identity":{
        |			"type":"object",
        |			"properties":{
        |				"identityId":{
        |					"type":"string"
        |				},
        |				"identityPoolId":{
        |					"type":"string"
        |				}
        |			},
        |			"additionalProperties":false
        |		}
        |	},
        |	"additionalProperties":false
        |}
      """.stripMargin)

    val jsonFile = JsonFile(sourceSchema, new File("1-0-0"))
    val stubFile: File = new File(".")
    val command = GenerateCommand(stubFile, stubFile, noHeader = true)

    val output = command.transformSelfDescribing(List(jsonFile))

    val expected = GenerateCommand.DdlOutput(List(TextFile(new File("com.amazon.aws.lambda/java_context_1.sql"), resultContent)))

    output must beEqualTo(expected)
  }

  def e2 = {
    val resultContent =
      """|CREATE TABLE IF NOT EXISTS java_context (
         |    "aws_request_id"                         VARCHAR(128) ENCODE ZSTD,
         |    "client_context.client.app_package_name" VARCHAR(128) ENCODE ZSTD,
         |    "client_context.client.app_title"        VARCHAR(128) ENCODE ZSTD,
         |    "client_context.client.app_version_code" VARCHAR(128) ENCODE ZSTD,
         |    "client_context.client.app_version_name" VARCHAR(128) ENCODE ZSTD,
         |    "client_context.custom"                  VARCHAR(128) ENCODE ZSTD,
         |    "client_context.environment"             VARCHAR(128) ENCODE ZSTD,
         |    "function_name"                          VARCHAR(128) ENCODE ZSTD,
         |    "identity.identity_id"                   VARCHAR(128) ENCODE ZSTD,
         |    "identity.identity_pool_id"              VARCHAR(128) ENCODE ZSTD,
         |    "log_group_name"                         VARCHAR(128) ENCODE ZSTD,
         |    "log_stream_name"                        VARCHAR(128) ENCODE ZSTD,
         |    "memory_limit_in_mb"                     BIGINT       ENCODE ZSTD,
         |    "remaining_time_millis"                  BIGINT       ENCODE ZSTD
         |);
         |
         |COMMENT ON TABLE java_context IS 'Source: java-context.json';""".stripMargin

    val sourceSchema = parse(
      """
        |{
        |	"description":"Schema for an AWS Lambda Java context object, http://docs.aws.amazon.com/lambda/latest/dg/java-context-object.html",
        |	"type":"object",
        |	"properties":{
        |		"functionName":{
        |			"type":"string"
        |		},
        |		"logStreamName":{
        |			"type":"string"
        |		},
        |		"awsRequestId":{
        |			"type":"string"
        |		},
        |		"remainingTimeMillis":{
        |			"type":"integer",
        |			"minimum":0
        |		},
        |		"logGroupName":{
        |			"type":"string"
        |		},
        |		"memoryLimitInMB":{
        |			"type":"integer",
        |			"minimum":0
        |		},
        |		"clientContext":{
        |			"type":"object",
        |			"properties":{
        |				"client":{
        |					"type":"object",
        |					"properties":{
        |						"appTitle":{
        |							"type":"string"
        |						},
        |						"appVersionName":{
        |							"type":"string"
        |						},
        |						"appVersionCode":{
        |							"type":"string"
        |						},
        |						"appPackageName":{
        |							"type":"string"
        |						}
        |					},
        |					"additionalProperties":false
        |				},
        |				"custom":{
        |					"type":"object",
        |					"patternProperties":{
        |						".*":{
        |							"type":"string"
        |						}
        |					}
        |				},
        |				"environment":{
        |					"type":"object",
        |					"patternProperties":{
        |						".*":{
        |							"type":"string"
        |						}
        |					}
        |				}
        |			},
        |			"additionalProperties":false
        |		},
        |		"identity":{
        |			"type":"object",
        |			"properties":{
        |				"identityId":{
        |					"type":"string"
        |				},
        |				"identityPoolId":{
        |					"type":"string"
        |				}
        |			},
        |			"additionalProperties":false
        |		}
        |	},
        |	"additionalProperties":false
        |}
      """.stripMargin)

    val jsonFile = JsonFile(sourceSchema, new File("java-context.json"))
    val stubFile: File = new File(".")
    val command = GenerateCommand(stubFile, stubFile, rawMode = true, noHeader = true, varcharSize = 128)

    val output = command.transformRaw(List(jsonFile))

    val expected = GenerateCommand.DdlOutput(List(TextFile(new File("./java_context.sql"), resultContent)))

    output must beEqualTo(expected)
  }

  def e3 = {
    val resultContent =
      """|CREATE SCHEMA IF NOT EXISTS snowplow;
         |
         |CREATE TABLE IF NOT EXISTS snowplow.com_amazon_aws_ec2_instance_identity_document_1 (
         |    "schema_vendor"        VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "schema_name"          VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "schema_format"        VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "schema_version"       VARCHAR(128)  ENCODE ZSTD NOT NULL,
         |    "root_id"              CHAR(36)      ENCODE RAW  NOT NULL,
         |    "root_tstamp"          TIMESTAMP     ENCODE ZSTD NOT NULL,
         |    "ref_root"             VARCHAR(255)  ENCODE ZSTD NOT NULL,
         |    "ref_tree"             VARCHAR(1500) ENCODE ZSTD NOT NULL,
         |    "ref_parent"           VARCHAR(255)  ENCODE ZSTD NOT NULL,
         |    "account_id"           VARCHAR(4096) ENCODE ZSTD,
         |    "architecture"         VARCHAR(4096) ENCODE ZSTD,
         |    "availability_zone"    VARCHAR(4096) ENCODE ZSTD,
         |    "billing_products"     VARCHAR(5000) ENCODE ZSTD,
         |    "devpay_product_codes" VARCHAR(5000) ENCODE ZSTD,
         |    "image_id"             CHAR(12)      ENCODE ZSTD,
         |    "instance_id"          VARCHAR(19)   ENCODE ZSTD,
         |    "instance_type"        VARCHAR(4096) ENCODE ZSTD,
         |    "kernel_id"            CHAR(12)      ENCODE ZSTD,
         |    "pending_time"         TIMESTAMP     ENCODE ZSTD,
         |    "private_ip"           VARCHAR(15)   ENCODE ZSTD,
         |    "ramdisk_id"           CHAR(12)      ENCODE ZSTD,
         |    "region"               VARCHAR(4096) ENCODE ZSTD,
         |    "version"              VARCHAR(4096) ENCODE ZSTD,
         |    FOREIGN KEY (root_id) REFERENCES snowplow.events(event_id)
         |)
         |DISTSTYLE KEY
         |DISTKEY (root_id)
         |SORTKEY (root_tstamp);
         |
         |COMMENT ON TABLE snowplow.com_amazon_aws_ec2_instance_identity_document_1 IS 'iglu:com.amazon.aws.ec2/instance_identity_document/jsonschema/1-0-0';""".stripMargin

    val sourceSchema = parse(
      """|{
         |  "$schema" : "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
         |  "self" : {
         |    "vendor" : "com.amazon.aws.ec2",
         |    "name" : "instance_identity_document",
         |    "version" : "1-0-0",
         |    "format" : "jsonschema"
         |  },
         |  "type" : "object",
         |  "properties" : {
         |    "instanceId" : {
         |      "type" : "string",
         |      "minLength" : 10,
         |      "maxLength" : 19
         |    },
         |    "devpayProductCodes" : {
         |      "type" : [ "array", "null" ],
         |      "items" : {
         |        "type" : "string"
         |      }
         |    },
         |    "billingProducts" : {
         |      "type" : [ "array", "null" ],
         |      "items" : {
         |        "type" : "string"
         |      }
         |    },
         |    "availabilityZone" : {
         |      "type" : "string"
         |    },
         |    "accountId" : {
         |      "type" : "string"
         |    },
         |    "ramdiskId" : {
         |      "type" : [ "string", "null" ],
         |      "minLength" : 12,
         |      "maxLength" : 12
         |    },
         |    "architecture" : {
         |      "type" : "string"
         |    },
         |    "instanceType" : {
         |      "type" : "string"
         |    },
         |    "version" : {
         |      "type" : "string"
         |    },
         |    "pendingTime" : {
         |      "type" : "string",
         |      "format" : "date-time"
         |    },
         |    "imageId" : {
         |      "type" : "string",
         |      "minLength" : 12,
         |      "maxLength" : 12
         |    },
         |    "privateIp" : {
         |      "type" : "string",
         |      "format" : "ipv4",
         |      "minLength" : 11,
         |      "maxLength" : 15
         |    },
         |    "region" : {
         |      "type" : "string"
         |    },
         |    "kernelId" : {
         |      "type" : [ "string", "null" ],
         |      "minLength" : 12,
         |      "maxLength" : 12
         |    }
         |  },
         |  "additionalProperties" : false
         |}""".stripMargin)


    val jsonFile = JsonFile(sourceSchema, new File("1-0-0"))
    val stubFile: File = new File(".")
    val command = GenerateCommand(stubFile, stubFile, noHeader = true, dbSchema = Some("snowplow"))

    val output = command.transformSelfDescribing(List(jsonFile))

    val expected = GenerateCommand.DdlOutput(
      List(TextFile(new File("com.amazon.aws.ec2/instance_identity_document_1.sql"), resultContent))
    )

    output must beEqualTo(expected)
  }

  def e4 = {
    val sourceSchema = parse(
      """|{
         |	"$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
         |	"description": "Schema for a AWS CloudFront web distribution access log event. Version released 01 Jul 2014",
         |	"self": {
         |		"vendor": "com.amazon.aws.cloudfront",
         |		"name": "wd_access_log",
         |		"format": "jsonschema",
         |		"version": "1-0-4"
         |	},
         |
         |	"type": "object",
         |	"properties": {
         |		"dateTime": {
         |			"type": "string",
         |			"format": "date-time"
         |		},
         |		"xEdgeLocation": {
         |			"type": ["string", "null"],
         |			"maxLength": 32
         |		},
         |		"scBytes": {
         |			"type": ["number", "null"]
         |		},
         |		"cIp": {
         |			"type": ["string", "null"],
         |			"maxLength": 45
         |		},
         |		"csMethod": {
         |			"type": ["string", "null"],
         |			"maxLength": 3
         |		},
         |		"csHost": {
         |			"type": ["string", "null"],
         |			"maxLength": 2000
         |		},
         |		"csUriStem": {
         |			"type": ["string", "null"],
         |			"maxLength": 8192
         |		},
         |		"scStatus": {
         |			"type": ["string", "null"],
         |			"maxLength": 3
         |		},
         |		"csReferer": {
         |			"type": ["string", "null"],
         |			"maxLength": 8192
         |		},
         |		"csUserAgent": {
         |			"type": ["string", "null"],
         |			"maxLength": 2000
         |		},
         |		"csUriQuery": {
         |			"type": ["string", "null"],
         |			"maxLength": 8192
         |		},
         |		"csCookie": {
         |			"type": ["string", "null"],
         |			"maxLength": 4096
         |		},
         |		"xEdgeResultType": {
         |			"type": ["string", "null"],
         |			"maxLength": 32
         |		},
         |		"xEdgeRequestId": {
         |			"type": ["string", "null"],
         |			"maxLength": 2000
         |		},
         |		"xHostHeader": {
         |			"type": ["string", "null"],
         |			"maxLength": 2000
         |		},
         |		"csProtocol": {
         |			"enum": ["http", "https", null]
         |		},
         |		"csBytes": {
         |			"type": ["number", "null"]
         |		},
         |		"timeTaken": {
         |			"type": ["number", "null"]
         |		},
         |		"xForwardedFor": {
         |			"type": ["string", "null"],
         |			"maxLength": 45
         |		},
         |		"sslProtocol": {
         |			"type": ["string", "null"],
         |			"maxLength": 32
         |		},
         |		"sslCipher": {
         |			"type": ["string", "null"],
         |			"maxLength": 64
         |		},
         |		"xEdgeResponseResultType": {
         |			"type": ["string", "null"],
         |			"maxLength": 32
         |		}
         |	},
         |	"required": ["dateTime"],
         |	"additionalProperties": false
         |}
         |""".stripMargin)


    val resultContent =
      """|{
         |    "jsonpaths": [
         |        "$.schema.vendor",
         |        "$.schema.name",
         |        "$.schema.format",
         |        "$.schema.version",
         |        "$.hierarchy.rootId",
         |        "$.hierarchy.rootTstamp",
         |        "$.hierarchy.refRoot",
         |        "$.hierarchy.refTree",
         |        "$.hierarchy.refParent",
         |        "$.data.dateTime",
         |        "$.data.cIp",
         |        "$.data.csBytes",
         |        "$.data.csCookie",
         |        "$.data.csHost",
         |        "$.data.csMethod",
         |        "$.data.csProtocol",
         |        "$.data.csReferer",
         |        "$.data.csUriQuery",
         |        "$.data.csUriStem",
         |        "$.data.csUserAgent",
         |        "$.data.scBytes",
         |        "$.data.scStatus",
         |        "$.data.sslCipher",
         |        "$.data.sslProtocol",
         |        "$.data.timeTaken",
         |        "$.data.xEdgeLocation",
         |        "$.data.xEdgeRequestId",
         |        "$.data.xEdgeResponseResultType",
         |        "$.data.xEdgeResultType",
         |        "$.data.xForwardedFor",
         |        "$.data.xHostHeader"
         |    ]
         |}""".stripMargin

    val jsonFile = JsonFile(sourceSchema, new File("1-0-0"))
    val stubFile: File = new File(".")
    val command = GenerateCommand(stubFile, stubFile, withJsonPaths = true)

    val output = command.transformSelfDescribing(List(jsonFile)).jsonPaths.head

    val expected = TextFile(new File("com.amazon.aws.cloudfront/wd_access_log_1.json"), resultContent)

    output must beEqualTo(expected)
  }

  def e5 = {
    import com.snowplowanalytics.iglu.ctl.GenerateCommand._
    import com.snowplowanalytics.iglu.schemaddl.redshift._
    import com.snowplowanalytics.iglu.schemaddl.redshift.generators._

    val definitions = List(
      TableDefinition("com.acme", "some_event", DdlFile(  // Definition with 2 product-type columns
        List(
          CommentBlock(Vector("AUTO-GENERATED BY schema-ddl DO NOT EDIT", "Generator: schema-ddl 0.2.0", "Generated: 2016-03-31 15:52")),
          CreateTable("some_event", List(
            Column("action", ProductType(List("warning1", "warning2"))),
            Column("time_stamp", RedshiftBigInt),
            Column("subject", ProductType(List("warning3")))
          )),
          CommentOn("some_event", "iglu:com.acme/some_event/jsonschema/1-0-0")
        )
      )),

      TableDefinition("com.acme", "some_context", DdlFile(  // Definition without warnings
        List(
          CommentBlock(Vector("AUTO-GENERATED BY schema-ddl DO NOT EDIT", "Generator: schema-ddl 0.2.0", "Generated: 2016-03-31 15:59")),
          CreateTable("some_context", List(
            Column("time_stamp", RedshiftBigInt)
          )),
          CommentOn("some_context", "iglu:com.acme/some_context/jsonschema/1-0-1")
        )
      )),

      TableDefinition("com.acme", "other_context_1", DdlFile( // Definition without iglu URI (impossible though)
        List(
          CommentBlock(Vector("AUTO-GENERATED BY schema-ddl DO NOT EDIT", "Generator: schema-ddl 0.2.0", "Generated: 2016-03-31 15:59")),
          CreateTable("other_context", List(
            Column("values", ProductType(List("another_warning")))
          ))
        )
      ))
    )

    getDdlWarnings(definitions) must beEqualTo(List(
      "Warning: in JSON Schema [iglu:com.acme/some_event/jsonschema/1-0-0]: warning1",
      "Warning: in JSON Schema [iglu:com.acme/some_event/jsonschema/1-0-0]: warning2",
      "Warning: in JSON Schema [iglu:com.acme/some_event/jsonschema/1-0-0]: warning3",
      "Warning: in generated DDL [com.acme/other_context_1]: another_warning"
    ))
  }

  def e6 = {
    val sourceSchema = parse(
      """
        |{
        |  "$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
        |  "description": "Schema for an example agency event",
        |  "self": {
        |    "vendor": "com.example-agency",
        |    "name": "cast",
        |    "format": "jsonschema",
        |    "version": "1-0-0"
        |  },
        |  "type": "object",
        |  "properties": {
        |    "name": {
        |      "type": "string"
        |    },
        |    "age": {
        |      "type": "number"
        |    }
        |  },
        |  "required":["name"]
        |}
      """.stripMargin
    )
    val sourceSchema2 = parse(
      """
        |{
        |  "$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
        |  "description": "Schema for an example agency event",
        |  "self": {
        |    "vendor": "com.example-agency",
        |    "name": "cast",
        |    "format": "jsonschema",
        |    "version": "1-0-3"
        |  },
        |  "type": "object",
        |  "properties": {
        |    "name": {
        |      "type": "string"
        |    },
        |    "surname": {
        |      "type": "string"
        |    },
        |    "age": {
        |      "type": "number"
        |    }
        |  },
        |  "required":["name", "surname"]
        |}
      """.stripMargin
    )
    val jsonFile = JsonFile(sourceSchema, new File("1-0-0"))
    val jsonFile2 = JsonFile(sourceSchema2, new File("1-0-3"))
    val stubFile: File = new File(".")
    val command = GenerateCommand(stubFile, stubFile)
    val (_, schemas) = splitValidations(List(jsonFile, jsonFile2).map(_.extractSelfDescribingSchema))
    val schemaVerValidation = command.validateSchemaVersions(schemas)

    val schemaVerMessages: List[String] = schemaVerValidation match {
      case Warnings(lst) => lst
      case Errors(lst) => lst
      case VersionSuccess(_) => List.empty[String]
    }

    schemaVerMessages must beEqualTo(List(
      s"Error: Directory [${stubFile.getAbsolutePath}] contains schemas of [com.example-agency/cast] which has gaps between schema versions." +
        " Use --force to switch off schema version check."
    ))
  }

  def e7 = {
    val sourceSchema = parse(
      """
        |{
        |  "$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
        |  "description": "Schema for an example agency event",
        |  "self": {
        |    "vendor": "com.example-agency",
        |    "name": "cast",
        |    "format": "jsonschema",
        |    "version": "1-1-0"
        |  },
        |  "type": "object",
        |  "properties": {
        |    "name": {
        |      "type": "string"
        |    },
        |    "age": {
        |      "type": "number"
        |    }
        |  },
        |  "required":["name"]
        |}
      """.stripMargin
    )
    val sourceSchema2 = parse(
      """
        |{
        |  "$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
        |  "description": "Schema for an example agency event",
        |  "self": {
        |    "vendor": "com.example-agency",
        |    "name": "cast",
        |    "format": "jsonschema",
        |    "version": "1-1-1"
        |  },
        |  "type": "object",
        |  "properties": {
        |    "name": {
        |      "type": "string"
        |    },
        |    "surname": {
        |      "type": "string"
        |    },
        |    "age": {
        |      "type": "number"
        |    }
        |  },
        |  "required":["name", "surname"]
        |}
      """.stripMargin
    )
    val jsonFile = JsonFile(sourceSchema, new File("1-1-0"))
    val jsonFile2 = JsonFile(sourceSchema2, new File("1-1-1"))
    val stubFile: File = new File(".")
    val command = GenerateCommand(stubFile, stubFile)
    val (_, schemas) = splitValidations(List(jsonFile, jsonFile2).map(_.extractSelfDescribingSchema))
    val schemaVerValidation = command.validateSchemaVersions(schemas)

    val schemaVerMessages: List[String] = schemaVerValidation match {
      case Warnings(lst) => lst
      case Errors(lst) => lst
      case VersionSuccess(_) => List.empty[String]
    }

    schemaVerMessages must beEqualTo(List(
      s"Error: Directory [${stubFile.getAbsolutePath}] contains schemas of [com.example-agency/cast] without version 1-0-0." +
        s" Use --force to switch off schema version check."
    ))
  }

  def e8 = {
    val sourceSchema = parse(
      """
        |{
        |      "$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
        |      "description": "Schema for custom contexts",
        |      "type": "object",
        |      "properties": {
        |                      "name": {
        |                      "type": "string"
        |                      },
        |                      "age": {
        |                      "type": "number"
        |                      }
        |      },
        |      "required":["name"]
        |}
      """.stripMargin)

    val resultContent =
      """|CREATE TABLE IF NOT EXISTS 1_0_0 (
         |    "name" VARCHAR(4096)    ENCODE ZSTD NOT NULL,
         |    "age"  DOUBLE PRECISION ENCODE RAW
         |);
         |
         |COMMENT ON TABLE 1_0_0 IS 'Source: 1-0-0';
         |
         |ALTER TABLE 1_0_0 OWNER TO storageloader;""".stripMargin

    val jsonFile = JsonFile(sourceSchema, new File("1-0-0"))
    val stubFile: File = new File(".")
    val command = GenerateCommand(stubFile, stubFile, rawMode = true, noHeader = true, owner = Some("storageloader"))
    val ddl = command.transformRaw(List(jsonFile))

    val expected = GenerateCommand.DdlOutput(
      List(TextFile(new File("./1_0_0.sql"), resultContent))
    )

    ddl must beEqualTo(expected)
  }
}
