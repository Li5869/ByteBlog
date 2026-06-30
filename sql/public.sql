/*
 Navicat Premium Dump SQL

 Source Server         : Aliyun
 Source Server Type    : PostgreSQL
 Source Server Version : 180003 (180003)
 Source Host           : 120.26.20.126:5432
 Source Catalog        : person_blog
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 180003 (180003)
 File Encoding         : 65001

 Date: 30/06/2026 15:54:36
*/


-- ----------------------------
-- Type structure for ghstore
-- ----------------------------
DROP TYPE IF EXISTS "public"."ghstore";
CREATE TYPE "public"."ghstore" (
  INPUT = "public"."ghstore_in",
  OUTPUT = "public"."ghstore_out",
  INTERNALLENGTH = VARIABLE,
  CATEGORY = U,
  DELIMITER = ','
);

-- ----------------------------
-- Type structure for halfvec
-- ----------------------------
DROP TYPE IF EXISTS "public"."halfvec";
CREATE TYPE "public"."halfvec" (
  INPUT = "public"."halfvec_in",
  OUTPUT = "public"."halfvec_out",
  RECEIVE = "public"."halfvec_recv",
  SEND = "public"."halfvec_send",
  TYPMOD_IN = "public"."halfvec_typmod_in",
  INTERNALLENGTH = VARIABLE,
  STORAGE = external,
  CATEGORY = U,
  DELIMITER = ','
);

-- ----------------------------
-- Type structure for hstore
-- ----------------------------
DROP TYPE IF EXISTS "public"."hstore";
CREATE TYPE "public"."hstore" (
  INPUT = "public"."hstore_in",
  OUTPUT = "public"."hstore_out",
  RECEIVE = "public"."hstore_recv",
  SEND = "public"."hstore_send",
  INTERNALLENGTH = VARIABLE,
  STORAGE = extended,
  CATEGORY = U,
  DELIMITER = ','
);

-- ----------------------------
-- Type structure for sparsevec
-- ----------------------------
DROP TYPE IF EXISTS "public"."sparsevec";
CREATE TYPE "public"."sparsevec" (
  INPUT = "public"."sparsevec_in",
  OUTPUT = "public"."sparsevec_out",
  RECEIVE = "public"."sparsevec_recv",
  SEND = "public"."sparsevec_send",
  TYPMOD_IN = "public"."sparsevec_typmod_in",
  INTERNALLENGTH = VARIABLE,
  STORAGE = external,
  CATEGORY = U,
  DELIMITER = ','
);

-- ----------------------------
-- Type structure for vector
-- ----------------------------
DROP TYPE IF EXISTS "public"."vector";
CREATE TYPE "public"."vector" (
  INPUT = "public"."vector_in",
  OUTPUT = "public"."vector_out",
  RECEIVE = "public"."vector_recv",
  SEND = "public"."vector_send",
  TYPMOD_IN = "public"."vector_typmod_in",
  INTERNALLENGTH = VARIABLE,
  STORAGE = external,
  CATEGORY = U,
  DELIMITER = ','
);

-- ----------------------------
-- Sequence structure for tb_admin_log_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_admin_log_id_seq";
CREATE SEQUENCE "public"."tb_admin_log_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_ai_conversation_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_ai_conversation_id_seq";
CREATE SEQUENCE "public"."tb_ai_conversation_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_ai_document_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_ai_document_id_seq";
CREATE SEQUENCE "public"."tb_ai_document_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_ai_message_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_ai_message_id_seq";
CREATE SEQUENCE "public"."tb_ai_message_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_article_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_article_id_seq";
CREATE SEQUENCE "public"."tb_article_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_article_like_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_article_like_id_seq";
CREATE SEQUENCE "public"."tb_article_like_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_article_tag_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_article_tag_id_seq";
CREATE SEQUENCE "public"."tb_article_tag_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_biz_notification_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_biz_notification_id_seq";
CREATE SEQUENCE "public"."tb_biz_notification_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_browse_history_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_browse_history_id_seq";
CREATE SEQUENCE "public"."tb_browse_history_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_category_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_category_id_seq";
CREATE SEQUENCE "public"."tb_category_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_collection_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_collection_id_seq";
CREATE SEQUENCE "public"."tb_collection_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_comment_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_comment_id_seq";
CREATE SEQUENCE "public"."tb_comment_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_comment_like_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_comment_like_id_seq";
CREATE SEQUENCE "public"."tb_comment_like_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_conversation_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_conversation_id_seq";
CREATE SEQUENCE "public"."tb_conversation_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_follow_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_follow_id_seq";
CREATE SEQUENCE "public"."tb_follow_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_knowledge_file_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_knowledge_file_id_seq";
CREATE SEQUENCE "public"."tb_knowledge_file_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_like_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_like_id_seq";
CREATE SEQUENCE "public"."tb_like_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_message_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_message_id_seq";
CREATE SEQUENCE "public"."tb_message_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_message_id_seq1
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_message_id_seq1";
CREATE SEQUENCE "public"."tb_message_id_seq1" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_mq_error_log_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_mq_error_log_id_seq";
CREATE SEQUENCE "public"."tb_mq_error_log_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_notification_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_notification_id_seq";
CREATE SEQUENCE "public"."tb_notification_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_sensitive_word_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_sensitive_word_id_seq";
CREATE SEQUENCE "public"."tb_sensitive_word_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_system_config_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_system_config_id_seq";
CREATE SEQUENCE "public"."tb_system_config_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_system_notification_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_system_notification_id_seq";
CREATE SEQUENCE "public"."tb_system_notification_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_tag_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_tag_id_seq";
CREATE SEQUENCE "public"."tb_tag_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tb_user_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."tb_user_id_seq";
CREATE SEQUENCE "public"."tb_user_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for blog_knowledge
-- ----------------------------
DROP TABLE IF EXISTS "public"."blog_knowledge";
CREATE TABLE "public"."blog_knowledge" (
  "langchain_id" uuid NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "embedding" "public"."vector" NOT NULL,
  "langchain_metadata" jsonb
)
;

-- ----------------------------
-- Table structure for knowledge_parent_chunks
-- ----------------------------
DROP TABLE IF EXISTS "public"."knowledge_parent_chunks";
CREATE TABLE "public"."knowledge_parent_chunks" (
  "id" uuid NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "metadata" jsonb DEFAULT '{}'::jsonb,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for skill_chunks
-- ----------------------------
DROP TABLE IF EXISTS "public"."skill_chunks";
CREATE TABLE "public"."skill_chunks" (
  "langchain_id" uuid NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "embedding" "public"."vector" NOT NULL,
  "langchain_metadata" json
)
;

-- ----------------------------
-- Table structure for tb_admin_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_admin_log";
CREATE TABLE "public"."tb_admin_log" (
  "id" int8 NOT NULL DEFAULT nextval('tb_admin_log_id_seq'::regclass),
  "admin_id" int8 NOT NULL,
  "action_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "target_type" varchar(50) COLLATE "pg_catalog"."default",
  "target_id" int8,
  "action_detail" text COLLATE "pg_catalog"."default",
  "ip_address" varchar(50) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "description" varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."tb_admin_log"."id" IS '日志ID';
COMMENT ON COLUMN "public"."tb_admin_log"."admin_id" IS '操作管理员ID(关联tb_user)';
COMMENT ON COLUMN "public"."tb_admin_log"."action_type" IS '操作类型: login/logout/create/update/delete/review等';
COMMENT ON COLUMN "public"."tb_admin_log"."target_type" IS '操作对象类型: article/user/comment/question等';
COMMENT ON COLUMN "public"."tb_admin_log"."target_id" IS '操作对象ID';
COMMENT ON COLUMN "public"."tb_admin_log"."action_detail" IS '操作详情/JSON格式';
COMMENT ON COLUMN "public"."tb_admin_log"."ip_address" IS '操作IP地址';
COMMENT ON COLUMN "public"."tb_admin_log"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_admin_log"."description" IS '操作描述';
COMMENT ON TABLE "public"."tb_admin_log" IS '管理员操作日志表';

-- ----------------------------
-- Table structure for tb_ai_conversation
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_ai_conversation";
CREATE TABLE "public"."tb_ai_conversation" (
  "id" int8 NOT NULL DEFAULT nextval('tb_ai_conversation_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "title" varchar(200) COLLATE "pg_catalog"."default",
  "message_count" int4 DEFAULT 0,
  "last_message" text COLLATE "pg_catalog"."default",
  "is_deleted" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_ai_conversation"."id" IS '会话ID';
COMMENT ON COLUMN "public"."tb_ai_conversation"."user_id" IS '用户ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_ai_conversation"."title" IS '会话标题';
COMMENT ON COLUMN "public"."tb_ai_conversation"."message_count" IS '消息总数';
COMMENT ON COLUMN "public"."tb_ai_conversation"."last_message" IS '最后一条消息内容';
COMMENT ON COLUMN "public"."tb_ai_conversation"."is_deleted" IS '逻辑删除标记';
COMMENT ON COLUMN "public"."tb_ai_conversation"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_ai_conversation"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_ai_conversation" IS 'AI会话表';

-- ----------------------------
-- Table structure for tb_ai_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_ai_message";
CREATE TABLE "public"."tb_ai_message" (
  "id" int8 NOT NULL DEFAULT nextval('tb_ai_message_id_seq'::regclass),
  "conversation_id" int8 NOT NULL,
  "role" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "tool_calls" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "thinking" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."tb_ai_message"."id" IS '消息ID';
COMMENT ON COLUMN "public"."tb_ai_message"."conversation_id" IS '会话ID，逻辑外键关联tb_ai_conversation';
COMMENT ON COLUMN "public"."tb_ai_message"."role" IS '角色：user-用户, assistant-助手, system-系统, tool-工具';
COMMENT ON COLUMN "public"."tb_ai_message"."content" IS '消息内容';
COMMENT ON COLUMN "public"."tb_ai_message"."tool_calls" IS '工具调用记录(JSON格式)';
COMMENT ON COLUMN "public"."tb_ai_message"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_ai_message"."thinking" IS '深度思考';
COMMENT ON TABLE "public"."tb_ai_message" IS 'AI消息表';

-- ----------------------------
-- Table structure for tb_article
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_article";
CREATE TABLE "public"."tb_article" (
  "id" int8 NOT NULL DEFAULT nextval('tb_article_id_seq'::regclass),
  "title" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "summary" varchar(500) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default",
  "cover" varchar(500) COLLATE "pg_catalog"."default",
  "author_id" int8 NOT NULL,
  "category_id" int8,
  "views" int8 DEFAULT 0,
  "likes" int8 DEFAULT 0,
  "comments" int8 DEFAULT 0,
  "collections" int8 DEFAULT 0,
  "is_top" bool DEFAULT false,
  "is_hot" bool DEFAULT false,
  "status" int2 DEFAULT 1,
  "is_deleted" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "review" varchar(10) COLLATE "pg_catalog"."default" NOT NULL DEFAULT ''::character varying
)
;
COMMENT ON COLUMN "public"."tb_article"."id" IS '文章ID';
COMMENT ON COLUMN "public"."tb_article"."title" IS '文章标题';
COMMENT ON COLUMN "public"."tb_article"."summary" IS '文章摘要';
COMMENT ON COLUMN "public"."tb_article"."content" IS '文章内容';
COMMENT ON COLUMN "public"."tb_article"."cover" IS '封面图片URL';
COMMENT ON COLUMN "public"."tb_article"."author_id" IS '作者ID(关联tb_user)';
COMMENT ON COLUMN "public"."tb_article"."category_id" IS '分类ID(关联tb_category)';
COMMENT ON COLUMN "public"."tb_article"."views" IS '浏览量';
COMMENT ON COLUMN "public"."tb_article"."likes" IS '点赞量';
COMMENT ON COLUMN "public"."tb_article"."comments" IS '评论量';
COMMENT ON COLUMN "public"."tb_article"."collections" IS '收藏量';
COMMENT ON COLUMN "public"."tb_article"."is_top" IS '是否置顶';
COMMENT ON COLUMN "public"."tb_article"."is_hot" IS '是否热门';
COMMENT ON COLUMN "public"."tb_article"."status" IS '状态: 0-草稿, 1-已发布';
COMMENT ON COLUMN "public"."tb_article"."is_deleted" IS '逻辑删除标记';
COMMENT ON COLUMN "public"."tb_article"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_article"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_article"."review" IS 'approved-通过,rejected-拒绝,pending-待审核';
COMMENT ON TABLE "public"."tb_article" IS '文章表';

-- ----------------------------
-- Table structure for tb_article_like
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_article_like";
CREATE TABLE "public"."tb_article_like" (
  "id" int8 NOT NULL DEFAULT nextval('tb_article_like_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "article_id" int8 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_article_like"."id" IS '点赞ID';
COMMENT ON COLUMN "public"."tb_article_like"."user_id" IS '用户ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_article_like"."article_id" IS '文章ID，逻辑外键关联tb_article';
COMMENT ON COLUMN "public"."tb_article_like"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_article_like" IS '文章点赞表';

-- ----------------------------
-- Table structure for tb_article_tag
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_article_tag";
CREATE TABLE "public"."tb_article_tag" (
  "id" int8 NOT NULL DEFAULT nextval('tb_article_tag_id_seq'::regclass),
  "article_id" int8 NOT NULL,
  "tag_id" int8 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_article_tag"."id" IS '关联ID';
COMMENT ON COLUMN "public"."tb_article_tag"."article_id" IS '文章ID(关联tb_article)';
COMMENT ON COLUMN "public"."tb_article_tag"."tag_id" IS '标签ID(关联tb_tag)';
COMMENT ON COLUMN "public"."tb_article_tag"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_article_tag" IS '文章标签关联表';

-- ----------------------------
-- Table structure for tb_biz_notification
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_biz_notification";
CREATE TABLE "public"."tb_biz_notification" (
  "id" int8 NOT NULL DEFAULT nextval('tb_biz_notification_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "action_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "target_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "target_id" int8 NOT NULL,
  "sender_id" int8 NOT NULL,
  "is_read" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "target_title" varchar(200) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default",
  "related_id" int8
)
;
COMMENT ON COLUMN "public"."tb_biz_notification"."id" IS '业务通知ID';
COMMENT ON COLUMN "public"."tb_biz_notification"."user_id" IS '接收通知的用户ID（被点赞/评论/关注的人）';
COMMENT ON COLUMN "public"."tb_biz_notification"."action_type" IS '行为类型: like-点赞, comment-评论, reply-回复, follow-关注, collection-收藏, answer-回答,adopt-采纳';
COMMENT ON COLUMN "public"."tb_biz_notification"."target_type" IS '目标类型: article-文章, comment-评论, question-问题, answer-回答, user-用户';
COMMENT ON COLUMN "public"."tb_biz_notification"."target_id" IS '目标ID（文章ID/评论ID/问题ID/回答ID/用户ID）';
COMMENT ON COLUMN "public"."tb_biz_notification"."sender_id" IS '发送者ID（触发通知的用户）';
COMMENT ON COLUMN "public"."tb_biz_notification"."is_read" IS '是否已读';
COMMENT ON COLUMN "public"."tb_biz_notification"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_biz_notification"."target_title" IS '目标标题(若有)';
COMMENT ON COLUMN "public"."tb_biz_notification"."content" IS '内容(reply,comment,answer有)';
COMMENT ON COLUMN "public"."tb_biz_notification"."related_id" IS '跳转关联内容id,用于通知界面跳转';
COMMENT ON TABLE "public"."tb_biz_notification" IS '业务通知表';

-- ----------------------------
-- Table structure for tb_browse_history
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_browse_history";
CREATE TABLE "public"."tb_browse_history" (
  "id" int8 NOT NULL DEFAULT nextval('tb_browse_history_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "article_id" int8,
  "browse_at" timestamp(6)
)
;
COMMENT ON COLUMN "public"."tb_browse_history"."id" IS '历史记录ID';
COMMENT ON COLUMN "public"."tb_browse_history"."user_id" IS '用户ID(关联tb_user)';
COMMENT ON COLUMN "public"."tb_browse_history"."article_id" IS '文章ID(关联tb_article)';
COMMENT ON COLUMN "public"."tb_browse_history"."browse_at" IS '浏览更新时间(用于排序)';
COMMENT ON TABLE "public"."tb_browse_history" IS '浏览历史记录表';

-- ----------------------------
-- Table structure for tb_category
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_category";
CREATE TABLE "public"."tb_category" (
  "id" int8 NOT NULL DEFAULT nextval('tb_category_id_seq'::regclass),
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sort" int4 DEFAULT 0,
  "articles_count" int8 DEFAULT 0,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_category"."id" IS '分类ID';
COMMENT ON COLUMN "public"."tb_category"."name" IS '分类名称';
COMMENT ON COLUMN "public"."tb_category"."sort" IS '排序(数字越小越靠前)';
COMMENT ON COLUMN "public"."tb_category"."articles_count" IS '文章数量';
COMMENT ON COLUMN "public"."tb_category"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_category"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_category" IS '分类表';

-- ----------------------------
-- Table structure for tb_collection
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_collection";
CREATE TABLE "public"."tb_collection" (
  "id" int8 NOT NULL DEFAULT nextval('tb_collection_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "article_id" int8 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_collection"."id" IS '收藏ID';
COMMENT ON COLUMN "public"."tb_collection"."user_id" IS '用户ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_collection"."article_id" IS '文章ID，逻辑外键关联tb_article';
COMMENT ON COLUMN "public"."tb_collection"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_collection" IS '收藏表';

-- ----------------------------
-- Table structure for tb_column
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_column";
CREATE TABLE "public"."tb_column" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "title" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "description" varchar(500) COLLATE "pg_catalog"."default",
  "cover" varchar(255) COLLATE "pg_catalog"."default",
  "articles_count" int4 DEFAULT 0,
  "views" int8 DEFAULT 0,
  "status" int4 DEFAULT 0,
  "is_deleted" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "subscription_count" int4 DEFAULT 0
)
;
COMMENT ON COLUMN "public"."tb_column"."id" IS '专栏ID';
COMMENT ON COLUMN "public"."tb_column"."user_id" IS '用户ID(关联tb_user)';
COMMENT ON COLUMN "public"."tb_column"."title" IS '专栏标题';
COMMENT ON COLUMN "public"."tb_column"."description" IS '专栏描述';
COMMENT ON COLUMN "public"."tb_column"."cover" IS '专栏封面URL';
COMMENT ON COLUMN "public"."tb_column"."articles_count" IS '文章数量';
COMMENT ON COLUMN "public"."tb_column"."views" IS '浏览量';
COMMENT ON COLUMN "public"."tb_column"."status" IS '状态: 0-草稿, 1-已发布';
COMMENT ON COLUMN "public"."tb_column"."is_deleted" IS '逻辑删除标记';
COMMENT ON COLUMN "public"."tb_column"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_column"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_column"."subscription_count" IS '订阅数量';
COMMENT ON TABLE "public"."tb_column" IS '专栏表';

-- ----------------------------
-- Table structure for tb_column_article
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_column_article";
CREATE TABLE "public"."tb_column_article" (
  "id" int8 NOT NULL,
  "column_id" int8 NOT NULL,
  "article_id" int8 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_column_article"."id" IS '主键ID';
COMMENT ON COLUMN "public"."tb_column_article"."column_id" IS '专栏ID';
COMMENT ON COLUMN "public"."tb_column_article"."article_id" IS '文章ID';
COMMENT ON COLUMN "public"."tb_column_article"."created_at" IS '添加时间';
COMMENT ON TABLE "public"."tb_column_article" IS '专栏文章关联表';

-- ----------------------------
-- Table structure for tb_column_subscription
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_column_subscription";
CREATE TABLE "public"."tb_column_subscription" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "column_id" int8 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_column_subscription"."id" IS '主键ID';
COMMENT ON COLUMN "public"."tb_column_subscription"."user_id" IS '订阅用户ID';
COMMENT ON COLUMN "public"."tb_column_subscription"."column_id" IS '订阅的专栏ID';
COMMENT ON COLUMN "public"."tb_column_subscription"."created_at" IS '订阅时间';
COMMENT ON TABLE "public"."tb_column_subscription" IS '专栏订阅表';

-- ----------------------------
-- Table structure for tb_comment
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_comment";
CREATE TABLE "public"."tb_comment" (
  "id" int8 NOT NULL DEFAULT nextval('tb_comment_id_seq'::regclass),
  "content" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL,
  "article_id" int8,
  "author_id" int8 NOT NULL,
  "parent_id" int8,
  "likes" int8 DEFAULT 0,
  "is_deleted" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'pending'::character varying,
  "reviewed_at" timestamp(6),
  "reviewer_id" int8,
  "is_anonymous" bool NOT NULL
)
;
COMMENT ON COLUMN "public"."tb_comment"."id" IS '评论ID';
COMMENT ON COLUMN "public"."tb_comment"."content" IS '评论内容';
COMMENT ON COLUMN "public"."tb_comment"."article_id" IS '文章ID，逻辑外键关联tb_article';
COMMENT ON COLUMN "public"."tb_comment"."author_id" IS '评论者ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_comment"."parent_id" IS '父评论ID，逻辑外键关联tb_comment，用于回复';
COMMENT ON COLUMN "public"."tb_comment"."likes" IS '点赞量';
COMMENT ON COLUMN "public"."tb_comment"."is_deleted" IS '逻辑删除标记: false-未删除, true-已删除';
COMMENT ON COLUMN "public"."tb_comment"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_comment"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_comment"."status" IS '审核状态: pending-待审核, approved-已通过, rejected-已拒绝';
COMMENT ON COLUMN "public"."tb_comment"."reviewed_at" IS '审核时间';
COMMENT ON COLUMN "public"."tb_comment"."reviewer_id" IS '审核人ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_comment"."is_anonymous" IS '是否匿名';
COMMENT ON TABLE "public"."tb_comment" IS '评论表';

-- ----------------------------
-- Table structure for tb_comment_like
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_comment_like";
CREATE TABLE "public"."tb_comment_like" (
  "id" int8 NOT NULL DEFAULT nextval('tb_comment_like_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "comment_id" int8 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_comment_like"."id" IS '点赞ID';
COMMENT ON COLUMN "public"."tb_comment_like"."user_id" IS '用户ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_comment_like"."comment_id" IS '评论ID，逻辑外键关联tb_comment';
COMMENT ON COLUMN "public"."tb_comment_like"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_comment_like" IS '评论点赞表';

-- ----------------------------
-- Table structure for tb_conversation
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_conversation";
CREATE TABLE "public"."tb_conversation" (
  "id" int8 NOT NULL DEFAULT nextval('tb_conversation_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "target_user_id" int8 NOT NULL,
  "last_message" varchar(1000) COLLATE "pg_catalog"."default",
  "last_message_time" timestamp(6),
  "unread_count" int4 DEFAULT 0,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_conversation"."id" IS '会话ID';
COMMENT ON COLUMN "public"."tb_conversation"."user_id" IS '用户ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_conversation"."target_user_id" IS '目标用户ID（对话的另一方），逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_conversation"."last_message" IS '最后一条消息内容';
COMMENT ON COLUMN "public"."tb_conversation"."last_message_time" IS '最后一条消息时间';
COMMENT ON COLUMN "public"."tb_conversation"."unread_count" IS '未读消息数';
COMMENT ON COLUMN "public"."tb_conversation"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_conversation"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_conversation" IS '私信会话表';

-- ----------------------------
-- Table structure for tb_coupon_template
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_coupon_template";
CREATE TABLE "public"."tb_coupon_template" (
  "id" int8 NOT NULL,
  "coupon_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "coupon_type" int2 NOT NULL DEFAULT 1,
  "claim_type" int2 NOT NULL DEFAULT 1,
  "discount_amount" int4,
  "discount_rate" numeric(3,2),
  "min_order_amount" int4 DEFAULT 0,
  "max_discount_amount" int4,
  "total_count" int4,
  "stock" int4,
  "claim_start_time" timestamp(6),
  "claim_end_time" timestamp(6),
  "start_time" timestamp(6),
  "end_time" timestamp(6),
  "valid_days" int4,
  "points_cost" int4 DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 1,
  "description" varchar(500) COLLATE "pg_catalog"."default",
  "creator_id" int8,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "is_deleted" bool DEFAULT false
)
;
COMMENT ON COLUMN "public"."tb_coupon_template"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_coupon_template"."coupon_name" IS '优惠券名称';
COMMENT ON COLUMN "public"."tb_coupon_template"."coupon_type" IS '类型：1-满减券 2-折扣券 3-无门槛券';
COMMENT ON COLUMN "public"."tb_coupon_template"."claim_type" IS '领取类型：1-免费领取 2-积分兑换';
COMMENT ON COLUMN "public"."tb_coupon_template"."discount_amount" IS '优惠金额（积分，整数）';
COMMENT ON COLUMN "public"."tb_coupon_template"."discount_rate" IS '折扣率（折扣券专用）';
COMMENT ON COLUMN "public"."tb_coupon_template"."min_order_amount" IS '最低消费金额（积分，整数）';
COMMENT ON COLUMN "public"."tb_coupon_template"."max_discount_amount" IS '最大优惠金额（积分，整数，折扣券封顶）';
COMMENT ON COLUMN "public"."tb_coupon_template"."total_count" IS '总库存（null表示不限量）';
COMMENT ON COLUMN "public"."tb_coupon_template"."stock" IS '剩余库存（不限量时为null）';
COMMENT ON COLUMN "public"."tb_coupon_template"."claim_start_time" IS '抢购开始时间（null表示上架即可抢）';
COMMENT ON COLUMN "public"."tb_coupon_template"."claim_end_time" IS '抢购结束时间（null表示不限时）';
COMMENT ON COLUMN "public"."tb_coupon_template"."start_time" IS '有效期开始时间（券本身的使用起始时间）';
COMMENT ON COLUMN "public"."tb_coupon_template"."end_time" IS '有效期结束时间（固定时间类型）';
COMMENT ON COLUMN "public"."tb_coupon_template"."valid_days" IS '有效天数（领取后N天类型）';
COMMENT ON COLUMN "public"."tb_coupon_template"."points_cost" IS '所需积分（0或null表示免费领取）';
COMMENT ON COLUMN "public"."tb_coupon_template"."status" IS '状态：0-下架 1-上架';
COMMENT ON COLUMN "public"."tb_coupon_template"."description" IS '使用说明';
COMMENT ON COLUMN "public"."tb_coupon_template"."creator_id" IS '创建者ID';
COMMENT ON COLUMN "public"."tb_coupon_template"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_coupon_template"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_coupon_template"."is_deleted" IS '是否删除';
COMMENT ON TABLE "public"."tb_coupon_template" IS '优惠券模板表';

-- ----------------------------
-- Table structure for tb_follow
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_follow";
CREATE TABLE "public"."tb_follow" (
  "id" int8 NOT NULL DEFAULT nextval('tb_follow_id_seq'::regclass),
  "follower_id" int8 NOT NULL,
  "following_id" int8 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_follow"."id" IS '关注ID';
COMMENT ON COLUMN "public"."tb_follow"."follower_id" IS '关注者ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_follow"."following_id" IS '被关注者ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_follow"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_follow" IS '关注表';

-- ----------------------------
-- Table structure for tb_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_knowledge_base";
CREATE TABLE "public"."tb_knowledge_base" (
  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "trigger_keywords" text[] COLLATE "pg_catalog"."default",
  "priority" int4 DEFAULT 0,
  "is_active" bool DEFAULT true,
  "created_at" timestamp(6) DEFAULT now(),
  "updated_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for tb_knowledge_file
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_knowledge_file";
CREATE TABLE "public"."tb_knowledge_file" (
  "id" int8 NOT NULL DEFAULT nextval('tb_knowledge_file_id_seq'::regclass),
  "file_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "original_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "description" varchar(500) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "file_url" varchar(1024) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "file_size" int8 NOT NULL DEFAULT 0,
  "chunk_count" int4 NOT NULL DEFAULT 0,
  "parent_ids" text COLLATE "pg_catalog"."default" DEFAULT '[]'::text,
  "source" varchar(50) COLLATE "pg_catalog"."default" DEFAULT 'file_upload'::character varying,
  "uploader_id" int8 NOT NULL DEFAULT 0,
  "created_at" timestamp(6) DEFAULT now(),
  "updated_at" timestamp(6) DEFAULT now(),
  "category" varchar(50) COLLATE "pg_catalog"."default" DEFAULT 'general'::character varying
)
;
COMMENT ON COLUMN "public"."tb_knowledge_file"."category" IS '知识库分类: project/interview/general';

-- ----------------------------
-- Table structure for tb_local_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_local_message";
CREATE TABLE "public"."tb_local_message" (
  "id" int8 NOT NULL,
  "biz_type" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "biz_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "biz_user_id" varchar(64) COLLATE "pg_catalog"."default",
  "exchange" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "routing_key" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "message_body" text COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "retry_count" int4 NOT NULL DEFAULT 0,
  "max_retry" int4 NOT NULL DEFAULT 5,
  "next_retry_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "is_deleted" bool DEFAULT false
)
;
COMMENT ON COLUMN "public"."tb_local_message"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_local_message"."biz_type" IS '业务类型：COUPON_CLAIM-优惠券领取 ORDER_CREATE-订单创建';
COMMENT ON COLUMN "public"."tb_local_message"."biz_id" IS '业务ID：优惠券模板ID/订单ID';
COMMENT ON COLUMN "public"."tb_local_message"."biz_user_id" IS '业务用户ID：用于幂等去重';
COMMENT ON COLUMN "public"."tb_local_message"."exchange" IS 'RabbitMQ交换机';
COMMENT ON COLUMN "public"."tb_local_message"."routing_key" IS 'RabbitMQ路由键';
COMMENT ON COLUMN "public"."tb_local_message"."message_body" IS '消息体JSON';
COMMENT ON COLUMN "public"."tb_local_message"."status" IS '状态：0-待发送 1-已完成 2-已失败';
COMMENT ON COLUMN "public"."tb_local_message"."retry_count" IS '已重试次数';
COMMENT ON COLUMN "public"."tb_local_message"."max_retry" IS '最大重试次数';
COMMENT ON COLUMN "public"."tb_local_message"."next_retry_time" IS '下次重试时间';
COMMENT ON COLUMN "public"."tb_local_message"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_local_message"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_local_message"."is_deleted" IS '逻辑删除';
COMMENT ON TABLE "public"."tb_local_message" IS '本地消息表（可靠消息投递）';

-- ----------------------------
-- Table structure for tb_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_message";
CREATE TABLE "public"."tb_message" (
  "id" int8 NOT NULL DEFAULT nextval('tb_message_id_seq1'::regclass),
  "sender_id" int8 NOT NULL,
  "receiver_id" int8 NOT NULL,
  "content" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL,
  "is_read" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_message"."id" IS '消息ID';
COMMENT ON COLUMN "public"."tb_message"."sender_id" IS '发送者ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_message"."receiver_id" IS '接收者ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_message"."content" IS '消息内容';
COMMENT ON COLUMN "public"."tb_message"."is_read" IS '是否已读: false-未读, true-已读';
COMMENT ON COLUMN "public"."tb_message"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_message" IS '私信消息表';

-- ----------------------------
-- Table structure for tb_mq_error_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_mq_error_log";
CREATE TABLE "public"."tb_mq_error_log" (
  "id" int8 NOT NULL DEFAULT nextval('tb_mq_error_log_id_seq'::regclass),
  "queue_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "exchange_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "routing_key" varchar(100) COLLATE "pg_catalog"."default",
  "message_body" text COLLATE "pg_catalog"."default" NOT NULL,
  "error_message" text COLLATE "pg_catalog"."default",
  "retry_count" int4 DEFAULT 0,
  "max_retry_count" int4 DEFAULT 3,
  "status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'PENDING'::character varying,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_mq_error_log"."queue_name" IS '原队列名称';
COMMENT ON COLUMN "public"."tb_mq_error_log"."exchange_name" IS '原交换机名称';
COMMENT ON COLUMN "public"."tb_mq_error_log"."routing_key" IS '原路由键';
COMMENT ON COLUMN "public"."tb_mq_error_log"."message_body" IS '消息体（JSON格式）';
COMMENT ON COLUMN "public"."tb_mq_error_log"."error_message" IS '最后一次错误信息';
COMMENT ON COLUMN "public"."tb_mq_error_log"."retry_count" IS '已重试次数';
COMMENT ON COLUMN "public"."tb_mq_error_log"."max_retry_count" IS '最大重试次数';
COMMENT ON COLUMN "public"."tb_mq_error_log"."status" IS '状态: PENDING-待处理 RESOLVED-已解决 FAILED-处理失败';
COMMENT ON COLUMN "public"."tb_mq_error_log"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_mq_error_log"."updated_at" IS '更新时间';

-- ----------------------------
-- Table structure for tb_order
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_order";
CREATE TABLE "public"."tb_order" (
  "id" int8 NOT NULL,
  "order_no" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8 NOT NULL,
  "biz_type" int2 NOT NULL DEFAULT 1,
  "biz_id" int8 NOT NULL,
  "biz_snapshot" varchar(500) COLLATE "pg_catalog"."default",
  "points_cost" int4 NOT NULL,
  "coupon_id" int8,
  "coupon_discount" int4 DEFAULT 0,
  "actual_points" int4 NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "expire_time" timestamp(6),
  "cancel_reason" varchar(100) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "is_deleted" bool DEFAULT false
)
;
COMMENT ON COLUMN "public"."tb_order"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_order"."order_no" IS '订单号（业务前缀+雪花ID，如VIP1929837465123456789）';
COMMENT ON COLUMN "public"."tb_order"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_order"."biz_type" IS '业务类型：1-VIP会员 2-付费专栏（预留）';
COMMENT ON COLUMN "public"."tb_order"."biz_id" IS '业务ID（套餐ID / 专栏ID）';
COMMENT ON COLUMN "public"."tb_order"."biz_snapshot" IS '业务快照（下单时的商品信息JSON字符串，防止后续改价影响历史订单）';
COMMENT ON COLUMN "public"."tb_order"."points_cost" IS '积分原价（套餐积分价格）';
COMMENT ON COLUMN "public"."tb_order"."coupon_id" IS '优惠券ID（可为空）';
COMMENT ON COLUMN "public"."tb_order"."coupon_discount" IS '优惠券减免积分数';
COMMENT ON COLUMN "public"."tb_order"."actual_points" IS '实际冻结积分（= points_cost - coupon_discount）';
COMMENT ON COLUMN "public"."tb_order"."status" IS '订单状态：0-待确认 1-已冻结 2-已完成 3-已取消 4-已关闭';
COMMENT ON COLUMN "public"."tb_order"."expire_time" IS '订单过期时间（30分钟，超时触发Cancel）';
COMMENT ON COLUMN "public"."tb_order"."cancel_reason" IS '取消原因';
COMMENT ON COLUMN "public"."tb_order"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_order"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_order"."is_deleted" IS '逻辑删除：false-未删除 true-已删除';
COMMENT ON TABLE "public"."tb_order" IS '通用订单表';

-- ----------------------------
-- Table structure for tb_point_log_202606
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_point_log_202606";
CREATE TABLE "public"."tb_point_log_202606" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "points" int4 NOT NULL,
  "type" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "biz_id" int8,
  "description" varchar(100) COLLATE "pg_catalog"."default",
  "operator_id" int8,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for tb_point_log_202607
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_point_log_202607";
CREATE TABLE "public"."tb_point_log_202607" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "points" int4 NOT NULL,
  "type" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "biz_id" int8,
  "description" varchar(100) COLLATE "pg_catalog"."default",
  "operator_id" int8,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for tb_point_log_202608
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_point_log_202608";
CREATE TABLE "public"."tb_point_log_202608" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "points" int4 NOT NULL,
  "type" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "biz_id" int8,
  "description" varchar(100) COLLATE "pg_catalog"."default",
  "operator_id" int8,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for tb_point_rank_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_point_rank_log";
CREATE TABLE "public"."tb_point_rank_log" (
  "id" int8 NOT NULL,
  "year_month" varchar(7) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8 NOT NULL,
  "points" int8 NOT NULL,
  "rank_num" int4 NOT NULL,
  "nickname" varchar(50) COLLATE "pg_catalog"."default",
  "avatar" varchar(500) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_point_rank_log"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_point_rank_log"."year_month" IS '年月（格式: 2026-06）';
COMMENT ON COLUMN "public"."tb_point_rank_log"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_point_rank_log"."points" IS '当月积分';
COMMENT ON COLUMN "public"."tb_point_rank_log"."rank_num" IS '排名';
COMMENT ON COLUMN "public"."tb_point_rank_log"."nickname" IS '用户昵称（冗余，历史快照）';
COMMENT ON COLUMN "public"."tb_point_rank_log"."avatar" IS '用户头像（冗余，历史快照）';
COMMENT ON COLUMN "public"."tb_point_rank_log"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_point_rank_log" IS '积分排行榜月度记录表';

-- ----------------------------
-- Table structure for tb_research_report
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_research_report";
CREATE TABLE "public"."tb_research_report" (
  "id" int8 NOT NULL,
  "task_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8 NOT NULL,
  "topic" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "report_url" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "summary" text COLLATE "pg_catalog"."default",
  "key_findings" text COLLATE "pg_catalog"."default",
  "sources" text COLLATE "pg_catalog"."default",
  "is_deleted" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_research_report"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_research_report"."task_id" IS '关联的研究任务UUID';
COMMENT ON COLUMN "public"."tb_research_report"."user_id" IS '用户ID，冗余字段便于查询';
COMMENT ON COLUMN "public"."tb_research_report"."topic" IS '研究主题';
COMMENT ON COLUMN "public"."tb_research_report"."report_url" IS 'OSS报告文件地址（Markdown格式）';
COMMENT ON COLUMN "public"."tb_research_report"."summary" IS '报告摘要，用于列表页预览';
COMMENT ON COLUMN "public"."tb_research_report"."key_findings" IS '关键发现列表JSON数组';
COMMENT ON COLUMN "public"."tb_research_report"."sources" IS '引用来源JSON数组，包含title和url';
COMMENT ON COLUMN "public"."tb_research_report"."is_deleted" IS '逻辑删除标记：false-正常/true-已删除';
COMMENT ON COLUMN "public"."tb_research_report"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_research_report" IS '深度研究报告表';

-- ----------------------------
-- Table structure for tb_research_task
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_research_task";
CREATE TABLE "public"."tb_research_task" (
  "id" int8 NOT NULL,
  "task_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8 NOT NULL,
  "topic" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "status" varchar(32) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'pending'::character varying,
  "plan" text COLLATE "pg_catalog"."default",
  "clarified_requirements" text COLLATE "pg_catalog"."default",
  "user_feedback" text COLLATE "pg_catalog"."default",
  "error_msg" text COLLATE "pg_catalog"."default",
  "is_deleted" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_research_task"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_research_task"."task_id" IS '任务UUID，前端生成，用于SSE连接标识';
COMMENT ON COLUMN "public"."tb_research_task"."user_id" IS '用户ID，逻辑外键关联tb_user';
COMMENT ON COLUMN "public"."tb_research_task"."topic" IS '研究主题';
COMMENT ON COLUMN "public"."tb_research_task"."status" IS '任务状态：pending-待处理/clarifying-澄清中/planning-规划中/executing-执行中/reporting-生成报告/completed-已完成/failed-失败/stopped-已停止';
COMMENT ON COLUMN "public"."tb_research_task"."plan" IS '研究计划JSON，含tasks数组和estimated_duration';
COMMENT ON COLUMN "public"."tb_research_task"."clarified_requirements" IS '用户澄清后的需求描述';
COMMENT ON COLUMN "public"."tb_research_task"."user_feedback" IS '用户对计划的反馈意见';
COMMENT ON COLUMN "public"."tb_research_task"."error_msg" IS '错误信息（失败时记录）';
COMMENT ON COLUMN "public"."tb_research_task"."is_deleted" IS '逻辑删除标记：false-正常/true-已删除';
COMMENT ON COLUMN "public"."tb_research_task"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_research_task"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_research_task" IS '深度研究任务表';

-- ----------------------------
-- Table structure for tb_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_sensitive_word";
CREATE TABLE "public"."tb_sensitive_word" (
  "id" int8 NOT NULL DEFAULT nextval('tb_sensitive_word_id_seq'::regclass),
  "word" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 DEFAULT 1,
  "creator_id" int8,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_sensitive_word"."id" IS '敏感词ID';
COMMENT ON COLUMN "public"."tb_sensitive_word"."word" IS '敏感词内容';
COMMENT ON COLUMN "public"."tb_sensitive_word"."status" IS '状态: 0-禁用, 1-启用';
COMMENT ON COLUMN "public"."tb_sensitive_word"."creator_id" IS '创建者ID(关联tb_user)';
COMMENT ON COLUMN "public"."tb_sensitive_word"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_sensitive_word"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_sensitive_word" IS '敏感词表';

-- ----------------------------
-- Table structure for tb_sign_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_sign_record";
CREATE TABLE "public"."tb_sign_record" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "sign_date" date NOT NULL,
  "points" int4 NOT NULL,
  "continuous_days" int4 DEFAULT 1,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_sign_record"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_sign_record"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_sign_record"."sign_date" IS '签到日期';
COMMENT ON COLUMN "public"."tb_sign_record"."points" IS '获得积分';
COMMENT ON COLUMN "public"."tb_sign_record"."continuous_days" IS '连续签到天数';
COMMENT ON COLUMN "public"."tb_sign_record"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_sign_record" IS '签到记录表';

-- ----------------------------
-- Table structure for tb_system_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_system_config";
CREATE TABLE "public"."tb_system_config" (
  "id" int8 NOT NULL DEFAULT nextval('tb_system_config_id_seq'::regclass),
  "config_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "config_value" text COLLATE "pg_catalog"."default",
  "config_type" varchar(50) COLLATE "pg_catalog"."default",
  "description" varchar(500) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_system_config"."id" IS '配置ID';
COMMENT ON COLUMN "public"."tb_system_config"."config_key" IS '配置键';
COMMENT ON COLUMN "public"."tb_system_config"."config_value" IS '配置值';
COMMENT ON COLUMN "public"."tb_system_config"."config_type" IS '配置类型: boolean/string/number/json';
COMMENT ON COLUMN "public"."tb_system_config"."description" IS '配置描述';
COMMENT ON COLUMN "public"."tb_system_config"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_system_config"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_system_config" IS '系统配置表';

-- ----------------------------
-- Table structure for tb_system_notification
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_system_notification";
CREATE TABLE "public"."tb_system_notification" (
  "id" int8 NOT NULL DEFAULT nextval('tb_system_notification_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "title" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "content" varchar(1000) COLLATE "pg_catalog"."default",
  "is_read" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_system_notification"."id" IS '系统通知ID';
COMMENT ON COLUMN "public"."tb_system_notification"."user_id" IS '接收通知的用户ID';
COMMENT ON COLUMN "public"."tb_system_notification"."title" IS '通知标题';
COMMENT ON COLUMN "public"."tb_system_notification"."content" IS '通知内容';
COMMENT ON COLUMN "public"."tb_system_notification"."is_read" IS '是否已读';
COMMENT ON COLUMN "public"."tb_system_notification"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_system_notification" IS '系统通知表';

-- ----------------------------
-- Table structure for tb_tag
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_tag";
CREATE TABLE "public"."tb_tag" (
  "id" int8 NOT NULL DEFAULT nextval('tb_tag_id_seq'::regclass),
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "use_count" int8 DEFAULT 0,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_tag"."id" IS '标签ID';
COMMENT ON COLUMN "public"."tb_tag"."name" IS '标签名称';
COMMENT ON COLUMN "public"."tb_tag"."use_count" IS '使用次数';
COMMENT ON COLUMN "public"."tb_tag"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_tag" IS '标签表';

-- ----------------------------
-- Table structure for tb_tcc_transaction
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_tcc_transaction";
CREATE TABLE "public"."tb_tcc_transaction" (
  "id" int8 NOT NULL,
  "xid" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'TRYING'::character varying,
  "biz_type" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "retry_count" int4 NOT NULL DEFAULT 0,
  "max_retry" int4 NOT NULL DEFAULT 5,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "is_deleted" bool DEFAULT false
)
;
COMMENT ON COLUMN "public"."tb_tcc_transaction"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."xid" IS '全局事务ID（格式：ORDER_{orderId}）';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."status" IS '事务状态：TRYING-尝试中 CONFIRMED-已确认 CANCELLED-已取消';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."biz_type" IS '业务类型（同一 xid 下唯一）：POINT_FREEZE-积分冻结 COUPON_FREEZE-优惠券冻结';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."retry_count" IS '补偿重试次数（超过 max_retry 告警人工处理）';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."max_retry" IS '最大重试次数';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_tcc_transaction"."is_deleted" IS '逻辑删除：false-未删除 true-已删除';
COMMENT ON TABLE "public"."tb_tcc_transaction" IS 'TCC 分布式事务记录表';

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_user";
CREATE TABLE "public"."tb_user" (
  "id" int8 NOT NULL DEFAULT nextval('tb_user_id_seq'::regclass),
  "username" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "email" varchar(100) COLLATE "pg_catalog"."default",
  "phone" varchar(20) COLLATE "pg_catalog"."default",
  "nickname" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "avatar" varchar(500) COLLATE "pg_catalog"."default",
  "bio" varchar(500) COLLATE "pg_catalog"."default",
  "gender" int2 DEFAULT 0,
  "articles_count" int8 DEFAULT 0,
  "fans_count" int8 DEFAULT 0,
  "following_count" int8 DEFAULT 0,
  "views_count" int8 DEFAULT 0,
  "likes_count" int8 DEFAULT 0,
  "collections_count" int8 DEFAULT 0,
  "comments_count" int8 DEFAULT 0,
  "status" int2 DEFAULT 1,
  "is_admin" bool DEFAULT false,
  "last_login_time" timestamp(6),
  "last_login_ip" varchar(50) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_user"."id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_user"."username" IS '用户名';
COMMENT ON COLUMN "public"."tb_user"."password" IS '密码(加密存储)';
COMMENT ON COLUMN "public"."tb_user"."email" IS '邮箱';
COMMENT ON COLUMN "public"."tb_user"."phone" IS '手机号';
COMMENT ON COLUMN "public"."tb_user"."nickname" IS '昵称';
COMMENT ON COLUMN "public"."tb_user"."avatar" IS '头像URL';
COMMENT ON COLUMN "public"."tb_user"."bio" IS '个人简介';
COMMENT ON COLUMN "public"."tb_user"."gender" IS '性别: 0-未知, 1-男, 2-女';
COMMENT ON COLUMN "public"."tb_user"."articles_count" IS '文章数量';
COMMENT ON COLUMN "public"."tb_user"."fans_count" IS '粉丝数量';
COMMENT ON COLUMN "public"."tb_user"."following_count" IS '关注数量';
COMMENT ON COLUMN "public"."tb_user"."views_count" IS '访问量';
COMMENT ON COLUMN "public"."tb_user"."likes_count" IS '获赞数量';
COMMENT ON COLUMN "public"."tb_user"."collections_count" IS '收藏数量';
COMMENT ON COLUMN "public"."tb_user"."comments_count" IS '评论数量';
COMMENT ON COLUMN "public"."tb_user"."status" IS '状态: 0-封禁, 1-正常';
COMMENT ON COLUMN "public"."tb_user"."is_admin" IS '是否管理员';
COMMENT ON COLUMN "public"."tb_user"."last_login_time" IS '最后登录时间';
COMMENT ON COLUMN "public"."tb_user"."last_login_ip" IS '最后登录IP';
COMMENT ON COLUMN "public"."tb_user"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_user"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_user" IS '用户表';

-- ----------------------------
-- Table structure for tb_user_coupon
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_user_coupon";
CREATE TABLE "public"."tb_user_coupon" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "coupon_template_id" int8 NOT NULL,
  "coupon_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "coupon_type" int2 NOT NULL,
  "discount_amount" int4,
  "discount_rate" numeric(3,2),
  "min_order_amount" int4,
  "status" int2 NOT NULL DEFAULT 0,
  "source_type" int2 NOT NULL,
  "obtain_time" timestamp(6) NOT NULL,
  "use_time" timestamp(6),
  "expire_time" timestamp(6) NOT NULL,
  "order_id" int8,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "is_deleted" bool DEFAULT false,
  "max_discount_amount" int4
)
;
COMMENT ON COLUMN "public"."tb_user_coupon"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_user_coupon"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_user_coupon"."coupon_template_id" IS '优惠券模板ID';
COMMENT ON COLUMN "public"."tb_user_coupon"."coupon_name" IS '优惠券名称（冗余）';
COMMENT ON COLUMN "public"."tb_user_coupon"."coupon_type" IS '类型（冗余）';
COMMENT ON COLUMN "public"."tb_user_coupon"."discount_amount" IS '优惠金额（积分，整数，冗余）';
COMMENT ON COLUMN "public"."tb_user_coupon"."discount_rate" IS '折扣率（冗余，折扣券专用）';
COMMENT ON COLUMN "public"."tb_user_coupon"."min_order_amount" IS '最低消费金额（积分，整数，冗余）';
COMMENT ON COLUMN "public"."tb_user_coupon"."status" IS '状态：0-未使用 1-已使用 2-已过期 3-已冻结';
COMMENT ON COLUMN "public"."tb_user_coupon"."source_type" IS '来源：1-免费领取 2-积分抢购 3-系统发放';
COMMENT ON COLUMN "public"."tb_user_coupon"."obtain_time" IS '领取时间';
COMMENT ON COLUMN "public"."tb_user_coupon"."use_time" IS '使用时间';
COMMENT ON COLUMN "public"."tb_user_coupon"."expire_time" IS '过期时间';
COMMENT ON COLUMN "public"."tb_user_coupon"."order_id" IS '使用的订单ID';
COMMENT ON COLUMN "public"."tb_user_coupon"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_user_coupon"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_user_coupon"."is_deleted" IS '是否删除';
COMMENT ON COLUMN "public"."tb_user_coupon"."max_discount_amount" IS '最大优惠金额（积分，整数，折扣券封顶，冗余）';
COMMENT ON TABLE "public"."tb_user_coupon" IS '用户优惠券表';

-- ----------------------------
-- Table structure for tb_user_point
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_user_point";
CREATE TABLE "public"."tb_user_point" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "total_points" int8 DEFAULT 0,
  "available_points" int8 DEFAULT 0,
  "frozen_points" int8 DEFAULT 0,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_user_point"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_user_point"."user_id" IS '用户ID（关联tb_user）';
COMMENT ON COLUMN "public"."tb_user_point"."total_points" IS '累计积分';
COMMENT ON COLUMN "public"."tb_user_point"."available_points" IS '可用积分';
COMMENT ON COLUMN "public"."tb_user_point"."frozen_points" IS '冻结积分（积分消费预留）';
COMMENT ON COLUMN "public"."tb_user_point"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_user_point"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_user_point" IS '用户积分表';

-- ----------------------------
-- Table structure for tb_vip_membership
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_vip_membership";
CREATE TABLE "public"."tb_vip_membership" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "vip_level" int2 NOT NULL DEFAULT 0,
  "start_time" timestamp(6),
  "end_time" timestamp(6),
  "total_months" int4 DEFAULT 0,
  "last_order_id" int8,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_vip_membership"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_vip_membership"."user_id" IS '用户ID（唯一）';
COMMENT ON COLUMN "public"."tb_vip_membership"."vip_level" IS '会员等级：0-普通 1-VIP';
COMMENT ON COLUMN "public"."tb_vip_membership"."start_time" IS '当前会员期开始时间';
COMMENT ON COLUMN "public"."tb_vip_membership"."end_time" IS '当前会员期到期时间';
COMMENT ON COLUMN "public"."tb_vip_membership"."total_months" IS '累计开通月数（用于判断老用户）';
COMMENT ON COLUMN "public"."tb_vip_membership"."last_order_id" IS '最近一笔订单ID';
COMMENT ON COLUMN "public"."tb_vip_membership"."created_at" IS '首次开通时间';
COMMENT ON COLUMN "public"."tb_vip_membership"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_vip_membership" IS '会员状态表';

-- ----------------------------
-- Table structure for tb_vip_plan
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_vip_plan";
CREATE TABLE "public"."tb_vip_plan" (
  "id" int8 NOT NULL,
  "plan_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "plan_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "duration_months" int4 NOT NULL,
  "points_price" int4 NOT NULL,
  "sort_order" int4 DEFAULT 0,
  "status" int2 NOT NULL DEFAULT 1,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_vip_plan"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_vip_plan"."plan_code" IS '套餐编码：MONTH-月卡 QUARTER-季卡 YEAR-年卡';
COMMENT ON COLUMN "public"."tb_vip_plan"."plan_name" IS '套餐名称';
COMMENT ON COLUMN "public"."tb_vip_plan"."duration_months" IS '会员时长（月）';
COMMENT ON COLUMN "public"."tb_vip_plan"."points_price" IS '积分价格';
COMMENT ON COLUMN "public"."tb_vip_plan"."sort_order" IS '排序权重（数值越小越靠前）';
COMMENT ON COLUMN "public"."tb_vip_plan"."status" IS '状态：0-下架 1-上架';
COMMENT ON COLUMN "public"."tb_vip_plan"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_vip_plan"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."tb_vip_plan" IS 'VIP套餐表';

-- ----------------------------
-- Table structure for tb_writing_draft
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_writing_draft";
CREATE TABLE "public"."tb_writing_draft" (
  "id" int8 NOT NULL,
  "task_id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "title" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "summary" varchar(500) COLLATE "pg_catalog"."default",
  "content" text COLLATE "pg_catalog"."default",
  "cover" varchar(255) COLLATE "pg_catalog"."default",
  "category_id" int8,
  "category_name" varchar(50) COLLATE "pg_catalog"."default",
  "tag_ids" text COLLATE "pg_catalog"."default",
  "tag_names" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_writing_draft"."id" IS '草稿ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_writing_draft"."task_id" IS '关联写作任务ID（tb_writing_task.id）';
COMMENT ON COLUMN "public"."tb_writing_draft"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_writing_draft"."title" IS 'AI生成的文章标题';
COMMENT ON COLUMN "public"."tb_writing_draft"."summary" IS 'AI生成的文章摘要';
COMMENT ON COLUMN "public"."tb_writing_draft"."content" IS 'AI生成的文章正文（Markdown）';
COMMENT ON COLUMN "public"."tb_writing_draft"."cover" IS '封面图片URL';
COMMENT ON COLUMN "public"."tb_writing_draft"."category_id" IS '分类ID（匹配已有分类时返回，否则为空）';
COMMENT ON COLUMN "public"."tb_writing_draft"."category_name" IS '分类名称（AI生成，可能不在已有分类中）';
COMMENT ON COLUMN "public"."tb_writing_draft"."tag_ids" IS '已有标签ID列表，逗号分隔（如 "1,2,3"）';
COMMENT ON COLUMN "public"."tb_writing_draft"."tag_names" IS '新标签名称列表，逗号分隔（需发布时自动创建）';
COMMENT ON TABLE "public"."tb_writing_draft" IS 'AI写作草稿表（用户发布或存草稿后删除）';

-- ----------------------------
-- Table structure for tb_writing_plan
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_writing_plan";
CREATE TABLE "public"."tb_writing_plan" (
  "id" int8 NOT NULL,
  "task_id" int8 NOT NULL,
  "version" int4 DEFAULT 1,
  "topic" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "target_audience" varchar(500) COLLATE "pg_catalog"."default",
  "key_points" text COLLATE "pg_catalog"."default",
  "writing_style" varchar(50) COLLATE "pg_catalog"."default",
  "estimated_length" varchar(20) COLLATE "pg_catalog"."default",
  "reference_keywords" text COLLATE "pg_catalog"."default",
  "structure" text COLLATE "pg_catalog"."default",
  "approval_status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'pending'::character varying,
  "user_feedback" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_writing_plan"."id" IS '计划ID';
COMMENT ON COLUMN "public"."tb_writing_plan"."task_id" IS '关联任务ID';
COMMENT ON COLUMN "public"."tb_writing_plan"."version" IS '计划版本（支持多次修改）';
COMMENT ON COLUMN "public"."tb_writing_plan"."topic" IS '文章主题';
COMMENT ON COLUMN "public"."tb_writing_plan"."target_audience" IS '目标读者';
COMMENT ON COLUMN "public"."tb_writing_plan"."key_points" IS '核心要点列表';
COMMENT ON COLUMN "public"."tb_writing_plan"."writing_style" IS '写作风格: 教程/科普/经验分享/深度分析/技术解读/随笔';
COMMENT ON COLUMN "public"."tb_writing_plan"."estimated_length" IS '预计篇幅: 短文(800字)/中文(1500字)/长文(3000字+)';
COMMENT ON COLUMN "public"."tb_writing_plan"."reference_keywords" IS '参考搜索关键词';
COMMENT ON COLUMN "public"."tb_writing_plan"."structure" IS '文章结构大纲';
COMMENT ON COLUMN "public"."tb_writing_plan"."approval_status" IS '审核状态: pending/approved/rejected';
COMMENT ON COLUMN "public"."tb_writing_plan"."user_feedback" IS '用户修改意见';
COMMENT ON TABLE "public"."tb_writing_plan" IS 'AI写作计划表';

-- ----------------------------
-- Table structure for tb_writing_reflection
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_writing_reflection";
CREATE TABLE "public"."tb_writing_reflection" (
  "id" int8 NOT NULL,
  "task_id" int8 NOT NULL,
  "score" numeric(3,1) NOT NULL,
  "completeness" numeric(3,1) NOT NULL,
  "structure" numeric(3,1) NOT NULL,
  "expression" numeric(3,1) NOT NULL,
  "practicality" numeric(3,1) NOT NULL,
  "format" numeric(3,1) NOT NULL,
  "strengths" text COLLATE "pg_catalog"."default",
  "weaknesses" text COLLATE "pg_catalog"."default",
  "suggestions" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."tb_writing_reflection"."id" IS '评价记录ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."task_id" IS '关联写作任务ID（tb_writing_task.id，1:1）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."score" IS '综合评分（0.0-10.0，加权计算）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."completeness" IS '完整性评分（权重30%，0.0-10.0）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."structure" IS '结构性评分（权重20%，0.0-10.0）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."expression" IS '表达质量评分（权重25%，0.0-10.0）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."practicality" IS '实用性评分（权重15%，0.0-10.0）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."format" IS '格式规范评分（权重10%，0.0-10.0）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."strengths" IS '优点列表（JSON数组，如 ["优点1","优点2"]）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."weaknesses" IS '不足列表（JSON数组，如 ["不足1","不足2"]）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."suggestions" IS '改进建议列表（JSON数组，如 ["建议1","建议2"]）';
COMMENT ON COLUMN "public"."tb_writing_reflection"."created_at" IS '最终评价时间';
COMMENT ON TABLE "public"."tb_writing_reflection" IS 'AI写作反思评价表（存储最终一轮的5维度评分和改进建议）';

-- ----------------------------
-- Table structure for tb_writing_task
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_writing_task";
CREATE TABLE "public"."tb_writing_task" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'planning'::character varying,
  "current_step" varchar(20) COLLATE "pg_catalog"."default",
  "user_request" text COLLATE "pg_catalog"."default" NOT NULL,
  "article_id" int8,
  "final_action" varchar(20) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "completed_at" timestamp(6),
  "revision_count" int4
)
;
COMMENT ON COLUMN "public"."tb_writing_task"."id" IS '任务ID';
COMMENT ON COLUMN "public"."tb_writing_task"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_writing_task"."status" IS '任务状态: planning/waiting_approval/executing/reflecting/finalized/cancelled';
COMMENT ON COLUMN "public"."tb_writing_task"."current_step" IS '当前步骤: plan_generated/title/summary/content/tags/reflected/completed';
COMMENT ON COLUMN "public"."tb_writing_task"."user_request" IS '用户原始写作需求';
COMMENT ON COLUMN "public"."tb_writing_task"."article_id" IS '生成的文章ID';
COMMENT ON COLUMN "public"."tb_writing_task"."final_action" IS '最终动作: save_draft/publish';
COMMENT ON COLUMN "public"."tb_writing_task"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."tb_writing_task"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."tb_writing_task"."completed_at" IS '完成时间';
COMMENT ON COLUMN "public"."tb_writing_task"."revision_count" IS '微调次数';
COMMENT ON TABLE "public"."tb_writing_task" IS 'AI写作任务表';

-- ----------------------------
-- Table structure for user_memories
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_memories";
CREATE TABLE "public"."user_memories" (
  "id" uuid NOT NULL,
  "vector" "public"."vector",
  "payload" jsonb
)
;

-- ----------------------------
-- Table structure for tb_point_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."tb_point_log";
CREATE TABLE "public"."tb_point_log" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "points" int4 NOT NULL,
  "type" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "biz_id" int8,
  "description" varchar(100) COLLATE "pg_catalog"."default",
  "operator_id" int8,
  "created_at" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
)
PARTITION BY RANGE (
  "created_at" "pg_catalog"."timestamp_ops"
)
;
ALTER TABLE "public"."tb_point_log" ATTACH PARTITION "public"."tb_point_log_202606" FOR VALUES FROM (
'2026-06-01 00:00:00'
) TO (
'2026-07-01 00:00:00'
)
;
ALTER TABLE "public"."tb_point_log" ATTACH PARTITION "public"."tb_point_log_202607" FOR VALUES FROM (
'2026-07-01 00:00:00'
) TO (
'2026-08-01 00:00:00'
)
;
ALTER TABLE "public"."tb_point_log" ATTACH PARTITION "public"."tb_point_log_202608" FOR VALUES FROM (
'2026-08-01 00:00:00'
) TO (
'2026-09-01 00:00:00'
)
;
COMMENT ON COLUMN "public"."tb_point_log"."id" IS '主键ID（雪花算法）';
COMMENT ON COLUMN "public"."tb_point_log"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."tb_point_log"."points" IS '积分变动值（正数增加，负数减少）';
COMMENT ON COLUMN "public"."tb_point_log"."type" IS '类型: sign/article_published/article_liked/article_collected/comment_liked/answer_liked/exchange/admin_adjust';
COMMENT ON COLUMN "public"."tb_point_log"."biz_id" IS '业务ID（可为null，如签到、管理员调整等场景）';
COMMENT ON COLUMN "public"."tb_point_log"."description" IS '管理员调整时的描述（type=admin_adjust时使用）';
COMMENT ON COLUMN "public"."tb_point_log"."operator_id" IS '操作人ID（管理员调整时使用）';
COMMENT ON COLUMN "public"."tb_point_log"."created_at" IS '创建时间';
COMMENT ON TABLE "public"."tb_point_log" IS '积分流水表';

-- ----------------------------
-- Function structure for akeys
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."akeys"("public"."hstore");
CREATE FUNCTION "public"."akeys"("public"."hstore")
  RETURNS "pg_catalog"."_text" AS '$libdir/hstore', 'hstore_akeys'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_halfvec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_halfvec"(_float4, int4, bool);
CREATE FUNCTION "public"."array_to_halfvec"(_float4, int4, bool)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'array_to_halfvec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_halfvec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_halfvec"(_int4, int4, bool);
CREATE FUNCTION "public"."array_to_halfvec"(_int4, int4, bool)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'array_to_halfvec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_halfvec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_halfvec"(_float8, int4, bool);
CREATE FUNCTION "public"."array_to_halfvec"(_float8, int4, bool)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'array_to_halfvec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_halfvec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_halfvec"(_numeric, int4, bool);
CREATE FUNCTION "public"."array_to_halfvec"(_numeric, int4, bool)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'array_to_halfvec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_sparsevec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_sparsevec"(_int4, int4, bool);
CREATE FUNCTION "public"."array_to_sparsevec"(_int4, int4, bool)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'array_to_sparsevec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_sparsevec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_sparsevec"(_numeric, int4, bool);
CREATE FUNCTION "public"."array_to_sparsevec"(_numeric, int4, bool)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'array_to_sparsevec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_sparsevec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_sparsevec"(_float4, int4, bool);
CREATE FUNCTION "public"."array_to_sparsevec"(_float4, int4, bool)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'array_to_sparsevec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_sparsevec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_sparsevec"(_float8, int4, bool);
CREATE FUNCTION "public"."array_to_sparsevec"(_float8, int4, bool)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'array_to_sparsevec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_vector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_vector"(_numeric, int4, bool);
CREATE FUNCTION "public"."array_to_vector"(_numeric, int4, bool)
  RETURNS "public"."vector" AS '$libdir/vector', 'array_to_vector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_vector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_vector"(_float8, int4, bool);
CREATE FUNCTION "public"."array_to_vector"(_float8, int4, bool)
  RETURNS "public"."vector" AS '$libdir/vector', 'array_to_vector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_vector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_vector"(_float4, int4, bool);
CREATE FUNCTION "public"."array_to_vector"(_float4, int4, bool)
  RETURNS "public"."vector" AS '$libdir/vector', 'array_to_vector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for array_to_vector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."array_to_vector"(_int4, int4, bool);
CREATE FUNCTION "public"."array_to_vector"(_int4, int4, bool)
  RETURNS "public"."vector" AS '$libdir/vector', 'array_to_vector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for avals
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."avals"("public"."hstore");
CREATE FUNCTION "public"."avals"("public"."hstore")
  RETURNS "pg_catalog"."_text" AS '$libdir/hstore', 'hstore_avals'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for binary_quantize
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."binary_quantize"("public"."halfvec");
CREATE FUNCTION "public"."binary_quantize"("public"."halfvec")
  RETURNS "pg_catalog"."bit" AS '$libdir/vector', 'halfvec_binary_quantize'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for binary_quantize
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."binary_quantize"("public"."vector");
CREATE FUNCTION "public"."binary_quantize"("public"."vector")
  RETURNS "pg_catalog"."bit" AS '$libdir/vector', 'binary_quantize'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for cosine_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."cosine_distance"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."cosine_distance"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'cosine_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for cosine_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."cosine_distance"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."cosine_distance"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'sparsevec_cosine_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for cosine_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."cosine_distance"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."cosine_distance"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_cosine_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for defined
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."defined"("public"."hstore", text);
CREATE FUNCTION "public"."defined"("public"."hstore", text)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_defined'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for delete
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."delete"("public"."hstore", text);
CREATE FUNCTION "public"."delete"("public"."hstore", text)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_delete'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for delete
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."delete"("public"."hstore", _text);
CREATE FUNCTION "public"."delete"("public"."hstore", _text)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_delete_array'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for delete
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."delete"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."delete"("public"."hstore", "public"."hstore")
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_delete_hstore'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for each
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."each"("hs" "public"."hstore", OUT "key" text, OUT "value" text);
CREATE FUNCTION "public"."each"(IN "hs" "public"."hstore", OUT "key" text, OUT "value" text)
  RETURNS SETOF "pg_catalog"."record" AS '$libdir/hstore', 'hstore_each'
  LANGUAGE c IMMUTABLE STRICT
  COST 1
  ROWS 1000;

-- ----------------------------
-- Function structure for exist
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."exist"("public"."hstore", text);
CREATE FUNCTION "public"."exist"("public"."hstore", text)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_exists'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for exists_all
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."exists_all"("public"."hstore", _text);
CREATE FUNCTION "public"."exists_all"("public"."hstore", _text)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_exists_all'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for exists_any
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."exists_any"("public"."hstore", _text);
CREATE FUNCTION "public"."exists_any"("public"."hstore", _text)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_exists_any'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for fetchval
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."fetchval"("public"."hstore", text);
CREATE FUNCTION "public"."fetchval"("public"."hstore", text)
  RETURNS "pg_catalog"."text" AS '$libdir/hstore', 'hstore_fetchval'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_compress
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_compress"(internal);
CREATE FUNCTION "public"."ghstore_compress"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'ghstore_compress'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_consistent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_consistent"(internal, "public"."hstore", int2, oid, internal);
CREATE FUNCTION "public"."ghstore_consistent"(internal, "public"."hstore", int2, oid, internal)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'ghstore_consistent'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_decompress
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_decompress"(internal);
CREATE FUNCTION "public"."ghstore_decompress"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'ghstore_decompress'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_in"(cstring);
CREATE FUNCTION "public"."ghstore_in"(cstring)
  RETURNS "public"."ghstore" AS '$libdir/hstore', 'ghstore_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_options
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_options"(internal);
CREATE FUNCTION "public"."ghstore_options"(internal)
  RETURNS "pg_catalog"."void" AS '$libdir/hstore', 'ghstore_options'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for ghstore_out
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_out"("public"."ghstore");
CREATE FUNCTION "public"."ghstore_out"("public"."ghstore")
  RETURNS "pg_catalog"."cstring" AS '$libdir/hstore', 'ghstore_out'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_penalty
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_penalty"(internal, internal, internal);
CREATE FUNCTION "public"."ghstore_penalty"(internal, internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'ghstore_penalty'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_picksplit
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_picksplit"(internal, internal);
CREATE FUNCTION "public"."ghstore_picksplit"(internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'ghstore_picksplit'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_same
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_same"("public"."ghstore", "public"."ghstore", internal);
CREATE FUNCTION "public"."ghstore_same"("public"."ghstore", "public"."ghstore", internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'ghstore_same'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ghstore_union
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ghstore_union"(internal, internal);
CREATE FUNCTION "public"."ghstore_union"(internal, internal)
  RETURNS "public"."ghstore" AS '$libdir/hstore', 'ghstore_union'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_consistent_hstore
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_consistent_hstore"(internal, int2, "public"."hstore", int4, internal, internal);
CREATE FUNCTION "public"."gin_consistent_hstore"(internal, int2, "public"."hstore", int4, internal, internal)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'gin_consistent_hstore'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_extract_hstore
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_extract_hstore"("public"."hstore", internal);
CREATE FUNCTION "public"."gin_extract_hstore"("public"."hstore", internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'gin_extract_hstore'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_extract_hstore_query
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_extract_hstore_query"("public"."hstore", internal, int2, internal, internal);
CREATE FUNCTION "public"."gin_extract_hstore_query"("public"."hstore", internal, int2, internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'gin_extract_hstore_query'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec"("public"."halfvec", int4, bool);
CREATE FUNCTION "public"."halfvec"("public"."halfvec", int4, bool)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_accum
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_accum"(_float8, "public"."halfvec");
CREATE FUNCTION "public"."halfvec_accum"(_float8, "public"."halfvec")
  RETURNS "pg_catalog"."_float8" AS '$libdir/vector', 'halfvec_accum'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_add
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_add"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_add"("public"."halfvec", "public"."halfvec")
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_add'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_avg
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_avg"(_float8);
CREATE FUNCTION "public"."halfvec_avg"(_float8)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_avg'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_cmp
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_cmp"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_cmp"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'halfvec_cmp'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_combine
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_combine"(_float8, _float8);
CREATE FUNCTION "public"."halfvec_combine"(_float8, _float8)
  RETURNS "pg_catalog"."_float8" AS '$libdir/vector', 'vector_combine'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_concat
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_concat"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_concat"("public"."halfvec", "public"."halfvec")
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_concat'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_eq
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_eq"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_eq"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'halfvec_eq'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_ge
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_ge"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_ge"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'halfvec_ge'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_gt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_gt"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_gt"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'halfvec_gt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_in"(cstring, oid, int4);
CREATE FUNCTION "public"."halfvec_in"(cstring, oid, int4)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_l2_squared_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_l2_squared_distance"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_l2_squared_distance"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_l2_squared_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_le
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_le"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_le"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'halfvec_le'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_lt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_lt"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_lt"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'halfvec_lt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_mul
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_mul"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_mul"("public"."halfvec", "public"."halfvec")
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_mul'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_ne
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_ne"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_ne"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'halfvec_ne'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_negative_inner_product
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_negative_inner_product"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_negative_inner_product"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_negative_inner_product'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_out
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_out"("public"."halfvec");
CREATE FUNCTION "public"."halfvec_out"("public"."halfvec")
  RETURNS "pg_catalog"."cstring" AS '$libdir/vector', 'halfvec_out'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_recv
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_recv"(internal, oid, int4);
CREATE FUNCTION "public"."halfvec_recv"(internal, oid, int4)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_recv'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_send
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_send"("public"."halfvec");
CREATE FUNCTION "public"."halfvec_send"("public"."halfvec")
  RETURNS "pg_catalog"."bytea" AS '$libdir/vector', 'halfvec_send'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_spherical_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_spherical_distance"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_spherical_distance"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_spherical_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_sub
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_sub"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."halfvec_sub"("public"."halfvec", "public"."halfvec")
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_sub'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_to_float4
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_to_float4"("public"."halfvec", int4, bool);
CREATE FUNCTION "public"."halfvec_to_float4"("public"."halfvec", int4, bool)
  RETURNS "pg_catalog"."_float4" AS '$libdir/vector', 'halfvec_to_float4'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_to_sparsevec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_to_sparsevec"("public"."halfvec", int4, bool);
CREATE FUNCTION "public"."halfvec_to_sparsevec"("public"."halfvec", int4, bool)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'halfvec_to_sparsevec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_to_vector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_to_vector"("public"."halfvec", int4, bool);
CREATE FUNCTION "public"."halfvec_to_vector"("public"."halfvec", int4, bool)
  RETURNS "public"."vector" AS '$libdir/vector', 'halfvec_to_vector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for halfvec_typmod_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."halfvec_typmod_in"(_cstring);
CREATE FUNCTION "public"."halfvec_typmod_in"(_cstring)
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'halfvec_typmod_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hamming_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hamming_distance"(bit, bit);
CREATE FUNCTION "public"."hamming_distance"(bit, bit)
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'hamming_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hnsw_bit_support
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hnsw_bit_support"(internal);
CREATE FUNCTION "public"."hnsw_bit_support"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/vector', 'hnsw_bit_support'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for hnsw_halfvec_support
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hnsw_halfvec_support"(internal);
CREATE FUNCTION "public"."hnsw_halfvec_support"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/vector', 'hnsw_halfvec_support'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for hnsw_sparsevec_support
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hnsw_sparsevec_support"(internal);
CREATE FUNCTION "public"."hnsw_sparsevec_support"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/vector', 'hnsw_sparsevec_support'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for hnswhandler
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hnswhandler"(internal);
CREATE FUNCTION "public"."hnswhandler"(internal)
  RETURNS "pg_catalog"."index_am_handler" AS '$libdir/vector', 'hnswhandler'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for hs_concat
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hs_concat"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hs_concat"("public"."hstore", "public"."hstore")
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_concat'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hs_contained
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hs_contained"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hs_contained"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_contained'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hs_contains
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hs_contains"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hs_contains"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_contains'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore"(record);
CREATE FUNCTION "public"."hstore"(record)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_from_record'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for hstore
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore"(text, text);
CREATE FUNCTION "public"."hstore"(text, text)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_from_text'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for hstore
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore"(_text, _text);
CREATE FUNCTION "public"."hstore"(_text, _text)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_from_arrays'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for hstore
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore"(_text);
CREATE FUNCTION "public"."hstore"(_text)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_from_array'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_cmp
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_cmp"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hstore_cmp"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."int4" AS '$libdir/hstore', 'hstore_cmp'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_eq
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_eq"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hstore_eq"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_eq'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_ge
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_ge"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hstore_ge"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_ge'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_gt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_gt"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hstore_gt"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_gt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_hash
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_hash"("public"."hstore");
CREATE FUNCTION "public"."hstore_hash"("public"."hstore")
  RETURNS "pg_catalog"."int4" AS '$libdir/hstore', 'hstore_hash'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_hash_extended
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_hash_extended"("public"."hstore", int8);
CREATE FUNCTION "public"."hstore_hash_extended"("public"."hstore", int8)
  RETURNS "pg_catalog"."int8" AS '$libdir/hstore', 'hstore_hash_extended'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_in"(cstring);
CREATE FUNCTION "public"."hstore_in"(cstring)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_le
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_le"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hstore_le"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_le'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_lt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_lt"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hstore_lt"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_lt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_ne
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_ne"("public"."hstore", "public"."hstore");
CREATE FUNCTION "public"."hstore_ne"("public"."hstore", "public"."hstore")
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_ne'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_out
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_out"("public"."hstore");
CREATE FUNCTION "public"."hstore_out"("public"."hstore")
  RETURNS "pg_catalog"."cstring" AS '$libdir/hstore', 'hstore_out'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_recv
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_recv"(internal);
CREATE FUNCTION "public"."hstore_recv"(internal)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_recv'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_send
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_send"("public"."hstore");
CREATE FUNCTION "public"."hstore_send"("public"."hstore")
  RETURNS "pg_catalog"."bytea" AS '$libdir/hstore', 'hstore_send'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_subscript_handler
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_subscript_handler"(internal);
CREATE FUNCTION "public"."hstore_subscript_handler"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/hstore', 'hstore_subscript_handler'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_to_array
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_to_array"("public"."hstore");
CREATE FUNCTION "public"."hstore_to_array"("public"."hstore")
  RETURNS "pg_catalog"."_text" AS '$libdir/hstore', 'hstore_to_array'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_to_json
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_to_json"("public"."hstore");
CREATE FUNCTION "public"."hstore_to_json"("public"."hstore")
  RETURNS "pg_catalog"."json" AS '$libdir/hstore', 'hstore_to_json'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_to_json_loose
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_to_json_loose"("public"."hstore");
CREATE FUNCTION "public"."hstore_to_json_loose"("public"."hstore")
  RETURNS "pg_catalog"."json" AS '$libdir/hstore', 'hstore_to_json_loose'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_to_jsonb
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_to_jsonb"("public"."hstore");
CREATE FUNCTION "public"."hstore_to_jsonb"("public"."hstore")
  RETURNS "pg_catalog"."jsonb" AS '$libdir/hstore', 'hstore_to_jsonb'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_to_jsonb_loose
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_to_jsonb_loose"("public"."hstore");
CREATE FUNCTION "public"."hstore_to_jsonb_loose"("public"."hstore")
  RETURNS "pg_catalog"."jsonb" AS '$libdir/hstore', 'hstore_to_jsonb_loose'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_to_matrix
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_to_matrix"("public"."hstore");
CREATE FUNCTION "public"."hstore_to_matrix"("public"."hstore")
  RETURNS "pg_catalog"."_text" AS '$libdir/hstore', 'hstore_to_matrix'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for hstore_version_diag
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."hstore_version_diag"("public"."hstore");
CREATE FUNCTION "public"."hstore_version_diag"("public"."hstore")
  RETURNS "pg_catalog"."int4" AS '$libdir/hstore', 'hstore_version_diag'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for inner_product
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."inner_product"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."inner_product"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'sparsevec_inner_product'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for inner_product
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."inner_product"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."inner_product"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_inner_product'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for inner_product
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."inner_product"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."inner_product"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'inner_product'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for isdefined
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."isdefined"("public"."hstore", text);
CREATE FUNCTION "public"."isdefined"("public"."hstore", text)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_defined'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for isexists
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."isexists"("public"."hstore", text);
CREATE FUNCTION "public"."isexists"("public"."hstore", text)
  RETURNS "pg_catalog"."bool" AS '$libdir/hstore', 'hstore_exists'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for ivfflat_bit_support
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ivfflat_bit_support"(internal);
CREATE FUNCTION "public"."ivfflat_bit_support"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/vector', 'ivfflat_bit_support'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for ivfflat_halfvec_support
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ivfflat_halfvec_support"(internal);
CREATE FUNCTION "public"."ivfflat_halfvec_support"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/vector', 'ivfflat_halfvec_support'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for ivfflathandler
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."ivfflathandler"(internal);
CREATE FUNCTION "public"."ivfflathandler"(internal)
  RETURNS "pg_catalog"."index_am_handler" AS '$libdir/vector', 'ivfflathandler'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for jaccard_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."jaccard_distance"(bit, bit);
CREATE FUNCTION "public"."jaccard_distance"(bit, bit)
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'jaccard_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l1_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l1_distance"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."l1_distance"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'l1_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l1_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l1_distance"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."l1_distance"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'sparsevec_l1_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l1_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l1_distance"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."l1_distance"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_l1_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_distance"("public"."halfvec", "public"."halfvec");
CREATE FUNCTION "public"."l2_distance"("public"."halfvec", "public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_l2_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_distance"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."l2_distance"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'l2_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_distance"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."l2_distance"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'sparsevec_l2_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_norm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_norm"("public"."halfvec");
CREATE FUNCTION "public"."l2_norm"("public"."halfvec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'halfvec_l2_norm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_norm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_norm"("public"."sparsevec");
CREATE FUNCTION "public"."l2_norm"("public"."sparsevec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'sparsevec_l2_norm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_normalize
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_normalize"("public"."halfvec");
CREATE FUNCTION "public"."l2_normalize"("public"."halfvec")
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_l2_normalize'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_normalize
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_normalize"("public"."sparsevec");
CREATE FUNCTION "public"."l2_normalize"("public"."sparsevec")
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'sparsevec_l2_normalize'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for l2_normalize
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."l2_normalize"("public"."vector");
CREATE FUNCTION "public"."l2_normalize"("public"."vector")
  RETURNS "public"."vector" AS '$libdir/vector', 'l2_normalize'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for populate_record
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."populate_record"(anyelement, "public"."hstore");
CREATE FUNCTION "public"."populate_record"(anyelement, "public"."hstore")
  RETURNS "pg_catalog"."anyelement" AS '$libdir/hstore', 'hstore_populate_record'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for skeys
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."skeys"("public"."hstore");
CREATE FUNCTION "public"."skeys"("public"."hstore")
  RETURNS SETOF "pg_catalog"."text" AS '$libdir/hstore', 'hstore_skeys'
  LANGUAGE c IMMUTABLE STRICT
  COST 1
  ROWS 1000;

-- ----------------------------
-- Function structure for slice
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."slice"("public"."hstore", _text);
CREATE FUNCTION "public"."slice"("public"."hstore", _text)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_slice_to_hstore'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for slice_array
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."slice_array"("public"."hstore", _text);
CREATE FUNCTION "public"."slice_array"("public"."hstore", _text)
  RETURNS "pg_catalog"."_text" AS '$libdir/hstore', 'hstore_slice_to_array'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec"("public"."sparsevec", int4, bool);
CREATE FUNCTION "public"."sparsevec"("public"."sparsevec", int4, bool)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'sparsevec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_cmp
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_cmp"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_cmp"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'sparsevec_cmp'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_eq
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_eq"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_eq"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'sparsevec_eq'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_ge
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_ge"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_ge"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'sparsevec_ge'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_gt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_gt"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_gt"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'sparsevec_gt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_in"(cstring, oid, int4);
CREATE FUNCTION "public"."sparsevec_in"(cstring, oid, int4)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'sparsevec_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_l2_squared_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_l2_squared_distance"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_l2_squared_distance"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'sparsevec_l2_squared_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_le
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_le"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_le"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'sparsevec_le'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_lt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_lt"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_lt"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'sparsevec_lt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_ne
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_ne"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_ne"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'sparsevec_ne'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_negative_inner_product
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_negative_inner_product"("public"."sparsevec", "public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_negative_inner_product"("public"."sparsevec", "public"."sparsevec")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'sparsevec_negative_inner_product'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_out
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_out"("public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_out"("public"."sparsevec")
  RETURNS "pg_catalog"."cstring" AS '$libdir/vector', 'sparsevec_out'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_recv
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_recv"(internal, oid, int4);
CREATE FUNCTION "public"."sparsevec_recv"(internal, oid, int4)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'sparsevec_recv'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_send
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_send"("public"."sparsevec");
CREATE FUNCTION "public"."sparsevec_send"("public"."sparsevec")
  RETURNS "pg_catalog"."bytea" AS '$libdir/vector', 'sparsevec_send'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_to_halfvec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_to_halfvec"("public"."sparsevec", int4, bool);
CREATE FUNCTION "public"."sparsevec_to_halfvec"("public"."sparsevec", int4, bool)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'sparsevec_to_halfvec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_to_vector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_to_vector"("public"."sparsevec", int4, bool);
CREATE FUNCTION "public"."sparsevec_to_vector"("public"."sparsevec", int4, bool)
  RETURNS "public"."vector" AS '$libdir/vector', 'sparsevec_to_vector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for sparsevec_typmod_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sparsevec_typmod_in"(_cstring);
CREATE FUNCTION "public"."sparsevec_typmod_in"(_cstring)
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'sparsevec_typmod_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for subvector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."subvector"("public"."vector", int4, int4);
CREATE FUNCTION "public"."subvector"("public"."vector", int4, int4)
  RETURNS "public"."vector" AS '$libdir/vector', 'subvector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for subvector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."subvector"("public"."halfvec", int4, int4);
CREATE FUNCTION "public"."subvector"("public"."halfvec", int4, int4)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'halfvec_subvector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for svals
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."svals"("public"."hstore");
CREATE FUNCTION "public"."svals"("public"."hstore")
  RETURNS SETOF "pg_catalog"."text" AS '$libdir/hstore', 'hstore_svals'
  LANGUAGE c IMMUTABLE STRICT
  COST 1
  ROWS 1000;

-- ----------------------------
-- Function structure for tconvert
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."tconvert"(text, text);
CREATE FUNCTION "public"."tconvert"(text, text)
  RETURNS "public"."hstore" AS '$libdir/hstore', 'hstore_from_text'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for update_column_subscription_count
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_column_subscription_count"();
CREATE FUNCTION "public"."update_column_subscription_count"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    -- 新增订阅
    IF TG_OP = 'INSERT' THEN
        UPDATE tb_column
        SET subscription_count = subscription_count + 1
        WHERE id = NEW.column_id;
        RETURN NEW;
    -- 取消订阅
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE tb_column
        SET subscription_count = GREATEST(subscription_count - 1, 0)
        WHERE id = OLD.column_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for update_updated_at_column
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_updated_at_column"();
CREATE FUNCTION "public"."update_updated_at_column"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for uuid_generate_v1
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_generate_v1"();
CREATE FUNCTION "public"."uuid_generate_v1"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v1'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v1mc
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_generate_v1mc"();
CREATE FUNCTION "public"."uuid_generate_v1mc"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v1mc'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v3
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_generate_v3"("namespace" uuid, "name" text);
CREATE FUNCTION "public"."uuid_generate_v3"("namespace" uuid, "name" text)
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v3'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v4
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_generate_v4"();
CREATE FUNCTION "public"."uuid_generate_v4"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v4'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v5
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_generate_v5"("namespace" uuid, "name" text);
CREATE FUNCTION "public"."uuid_generate_v5"("namespace" uuid, "name" text)
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v5'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_nil
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_nil"();
CREATE FUNCTION "public"."uuid_nil"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_nil'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_dns
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_ns_dns"();
CREATE FUNCTION "public"."uuid_ns_dns"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_dns'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_oid
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_ns_oid"();
CREATE FUNCTION "public"."uuid_ns_oid"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_oid'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_url
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_ns_url"();
CREATE FUNCTION "public"."uuid_ns_url"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_url'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_x500
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."uuid_ns_x500"();
CREATE FUNCTION "public"."uuid_ns_x500"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_x500'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector"("public"."vector", int4, bool);
CREATE FUNCTION "public"."vector"("public"."vector", int4, bool)
  RETURNS "public"."vector" AS '$libdir/vector', 'vector'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_accum
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_accum"(_float8, "public"."vector");
CREATE FUNCTION "public"."vector_accum"(_float8, "public"."vector")
  RETURNS "pg_catalog"."_float8" AS '$libdir/vector', 'vector_accum'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_add
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_add"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_add"("public"."vector", "public"."vector")
  RETURNS "public"."vector" AS '$libdir/vector', 'vector_add'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_avg
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_avg"(_float8);
CREATE FUNCTION "public"."vector_avg"(_float8)
  RETURNS "public"."vector" AS '$libdir/vector', 'vector_avg'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_cmp
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_cmp"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_cmp"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'vector_cmp'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_combine
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_combine"(_float8, _float8);
CREATE FUNCTION "public"."vector_combine"(_float8, _float8)
  RETURNS "pg_catalog"."_float8" AS '$libdir/vector', 'vector_combine'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_concat
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_concat"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_concat"("public"."vector", "public"."vector")
  RETURNS "public"."vector" AS '$libdir/vector', 'vector_concat'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_dims
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_dims"("public"."halfvec");
CREATE FUNCTION "public"."vector_dims"("public"."halfvec")
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'halfvec_vector_dims'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_dims
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_dims"("public"."vector");
CREATE FUNCTION "public"."vector_dims"("public"."vector")
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'vector_dims'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_eq
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_eq"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_eq"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'vector_eq'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_ge
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_ge"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_ge"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'vector_ge'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_gt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_gt"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_gt"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'vector_gt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_in"(cstring, oid, int4);
CREATE FUNCTION "public"."vector_in"(cstring, oid, int4)
  RETURNS "public"."vector" AS '$libdir/vector', 'vector_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_l2_squared_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_l2_squared_distance"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_l2_squared_distance"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'vector_l2_squared_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_le
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_le"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_le"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'vector_le'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_lt
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_lt"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_lt"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'vector_lt'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_mul
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_mul"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_mul"("public"."vector", "public"."vector")
  RETURNS "public"."vector" AS '$libdir/vector', 'vector_mul'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_ne
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_ne"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_ne"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."bool" AS '$libdir/vector', 'vector_ne'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_negative_inner_product
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_negative_inner_product"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_negative_inner_product"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'vector_negative_inner_product'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_norm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_norm"("public"."vector");
CREATE FUNCTION "public"."vector_norm"("public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'vector_norm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_out
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_out"("public"."vector");
CREATE FUNCTION "public"."vector_out"("public"."vector")
  RETURNS "pg_catalog"."cstring" AS '$libdir/vector', 'vector_out'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_recv
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_recv"(internal, oid, int4);
CREATE FUNCTION "public"."vector_recv"(internal, oid, int4)
  RETURNS "public"."vector" AS '$libdir/vector', 'vector_recv'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_send
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_send"("public"."vector");
CREATE FUNCTION "public"."vector_send"("public"."vector")
  RETURNS "pg_catalog"."bytea" AS '$libdir/vector', 'vector_send'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_spherical_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_spherical_distance"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_spherical_distance"("public"."vector", "public"."vector")
  RETURNS "pg_catalog"."float8" AS '$libdir/vector', 'vector_spherical_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_sub
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_sub"("public"."vector", "public"."vector");
CREATE FUNCTION "public"."vector_sub"("public"."vector", "public"."vector")
  RETURNS "public"."vector" AS '$libdir/vector', 'vector_sub'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_to_float4
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_to_float4"("public"."vector", int4, bool);
CREATE FUNCTION "public"."vector_to_float4"("public"."vector", int4, bool)
  RETURNS "pg_catalog"."_float4" AS '$libdir/vector', 'vector_to_float4'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_to_halfvec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_to_halfvec"("public"."vector", int4, bool);
CREATE FUNCTION "public"."vector_to_halfvec"("public"."vector", int4, bool)
  RETURNS "public"."halfvec" AS '$libdir/vector', 'vector_to_halfvec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_to_sparsevec
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_to_sparsevec"("public"."vector", int4, bool);
CREATE FUNCTION "public"."vector_to_sparsevec"("public"."vector", int4, bool)
  RETURNS "public"."sparsevec" AS '$libdir/vector', 'vector_to_sparsevec'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for vector_typmod_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."vector_typmod_in"(_cstring);
CREATE FUNCTION "public"."vector_typmod_in"(_cstring)
  RETURNS "pg_catalog"."int4" AS '$libdir/vector', 'vector_typmod_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_admin_log_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_ai_conversation_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_ai_document_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_ai_message_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_article_id_seq"', 10, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_article_like_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_article_tag_id_seq"', 20, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."tb_biz_notification_id_seq"
OWNED BY "public"."tb_biz_notification"."id";
SELECT setval('"public"."tb_biz_notification_id_seq"', 35, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_browse_history_id_seq"', 5, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_category_id_seq"', 6, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_collection_id_seq"', 4, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_comment_id_seq"', 8, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_comment_like_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."tb_conversation_id_seq"
OWNED BY "public"."tb_conversation"."id";
SELECT setval('"public"."tb_conversation_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_follow_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."tb_knowledge_file_id_seq"
OWNED BY "public"."tb_knowledge_file"."id";
SELECT setval('"public"."tb_knowledge_file_id_seq"', 16, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_like_id_seq"', 6, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_message_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."tb_message_id_seq1"
OWNED BY "public"."tb_message"."id";
SELECT setval('"public"."tb_message_id_seq1"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."tb_mq_error_log_id_seq"
OWNED BY "public"."tb_mq_error_log"."id";
SELECT setval('"public"."tb_mq_error_log_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_notification_id_seq"', 5, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_sensitive_word_id_seq"', 5, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_system_config_id_seq"', 4, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."tb_system_notification_id_seq"
OWNED BY "public"."tb_system_notification"."id";
SELECT setval('"public"."tb_system_notification_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."tb_tag_id_seq"', 15, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."tb_user_id_seq"
OWNED BY "public"."tb_user"."id";
SELECT setval('"public"."tb_user_id_seq"', 1, true);

-- ----------------------------
-- Indexes structure for table blog_knowledge
-- ----------------------------
CREATE INDEX "idx_blog_knowledge_category" ON "public"."blog_knowledge" USING gin (
  (langchain_metadata -> 'category'::text) "pg_catalog"."jsonb_path_ops"
);

-- ----------------------------
-- Primary Key structure for table blog_knowledge
-- ----------------------------
ALTER TABLE "public"."blog_knowledge" ADD CONSTRAINT "blog_knowledge_pkey" PRIMARY KEY ("langchain_id");

-- ----------------------------
-- Indexes structure for table knowledge_parent_chunks
-- ----------------------------
CREATE INDEX "idx_parent_chunks_category" ON "public"."knowledge_parent_chunks" USING gin (
  (metadata -> 'category'::text) "pg_catalog"."jsonb_path_ops"
);

-- ----------------------------
-- Primary Key structure for table knowledge_parent_chunks
-- ----------------------------
ALTER TABLE "public"."knowledge_parent_chunks" ADD CONSTRAINT "knowledge_parent_chunks_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table skill_chunks
-- ----------------------------
ALTER TABLE "public"."skill_chunks" ADD CONSTRAINT "skill_chunks_pkey" PRIMARY KEY ("langchain_id");

-- ----------------------------
-- Indexes structure for table tb_admin_log
-- ----------------------------
CREATE INDEX "idx_admin_log_action_type" ON "public"."tb_admin_log" USING btree (
  "action_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_admin_log_admin_id" ON "public"."tb_admin_log" USING btree (
  "admin_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_admin_log_created_at" ON "public"."tb_admin_log" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_admin_log_target" ON "public"."tb_admin_log" USING btree (
  "target_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "target_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_admin_log
-- ----------------------------
ALTER TABLE "public"."tb_admin_log" ADD CONSTRAINT "tb_admin_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_ai_conversation
-- ----------------------------
CREATE INDEX "idx_ai_conversation_created_at" ON "public"."tb_ai_conversation" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_ai_conversation_user_id" ON "public"."tb_ai_conversation" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_ai_conversation
-- ----------------------------
ALTER TABLE "public"."tb_ai_conversation" ADD CONSTRAINT "pk_ai_conversation" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_ai_message
-- ----------------------------
CREATE INDEX "idx_ai_message_conversation_id" ON "public"."tb_ai_message" USING btree (
  "conversation_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_ai_message_created_at" ON "public"."tb_ai_message" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_ai_message
-- ----------------------------
ALTER TABLE "public"."tb_ai_message" ADD CONSTRAINT "pk_ai_message" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_article
-- ----------------------------
CREATE INDEX "idx_article_author_id" ON "public"."tb_article" USING btree (
  "author_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_category_id" ON "public"."tb_article" USING btree (
  "category_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_created_at" ON "public"."tb_article" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_is_deleted" ON "public"."tb_article" USING btree (
  "is_deleted" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_is_hot" ON "public"."tb_article" USING btree (
  "is_hot" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_is_top" ON "public"."tb_article" USING btree (
  "is_top" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_likes" ON "public"."tb_article" USING btree (
  "likes" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_status" ON "public"."tb_article" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_top_filter" ON "public"."tb_article" USING btree (
  "is_top" "pg_catalog"."bool_ops" DESC NULLS FIRST,
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST,
  "is_deleted" "pg_catalog"."bool_ops" ASC NULLS LAST,
  "review" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "is_hot" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_views" ON "public"."tb_article" USING btree (
  "views" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_article
-- ----------------------------
CREATE TRIGGER "update_tb_article_updated_at" BEFORE UPDATE ON "public"."tb_article"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Primary Key structure for table tb_article
-- ----------------------------
ALTER TABLE "public"."tb_article" ADD CONSTRAINT "tb_article_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_article_like
-- ----------------------------
CREATE INDEX "idx_article_like_article_id" ON "public"."tb_article_like" USING btree (
  "article_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_like_created_at" ON "public"."tb_article_like" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_like_user_id" ON "public"."tb_article_like" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_article_like
-- ----------------------------
ALTER TABLE "public"."tb_article_like" ADD CONSTRAINT "uk_article_like_user_article" UNIQUE ("user_id", "article_id");

-- ----------------------------
-- Primary Key structure for table tb_article_like
-- ----------------------------
ALTER TABLE "public"."tb_article_like" ADD CONSTRAINT "tb_article_like_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_article_tag
-- ----------------------------
CREATE INDEX "idx_article_tag_article_id" ON "public"."tb_article_tag" USING btree (
  "article_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_tag_created_at" ON "public"."tb_article_tag" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_article_tag_tag_id" ON "public"."tb_article_tag" USING btree (
  "tag_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_article_tag
-- ----------------------------
ALTER TABLE "public"."tb_article_tag" ADD CONSTRAINT "tb_article_tag_article_id_tag_id_key" UNIQUE ("article_id", "tag_id");

-- ----------------------------
-- Primary Key structure for table tb_article_tag
-- ----------------------------
ALTER TABLE "public"."tb_article_tag" ADD CONSTRAINT "tb_article_tag_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_biz_notification
-- ----------------------------
CREATE INDEX "idx_biz_notification_action_type" ON "public"."tb_biz_notification" USING btree (
  "action_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_biz_notification_created_at" ON "public"."tb_biz_notification" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_biz_notification_is_read" ON "public"."tb_biz_notification" USING btree (
  "is_read" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_biz_notification_sender_id" ON "public"."tb_biz_notification" USING btree (
  "sender_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_biz_notification_target_type" ON "public"."tb_biz_notification" USING btree (
  "target_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_biz_notification_user_created" ON "public"."tb_biz_notification" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_biz_notification_user_id" ON "public"."tb_biz_notification" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_biz_notification_user_unread" ON "public"."tb_biz_notification" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "is_read" "pg_catalog"."bool_ops" ASC NULLS LAST
) WHERE is_read = false;

-- ----------------------------
-- Primary Key structure for table tb_biz_notification
-- ----------------------------
ALTER TABLE "public"."tb_biz_notification" ADD CONSTRAINT "tb_biz_notification_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_browse_history
-- ----------------------------
CREATE INDEX "idx_browse_history_article_id" ON "public"."tb_browse_history" USING btree (
  "article_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_browse_history_user_article" ON "public"."tb_browse_history" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "article_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_browse_history_user_id" ON "public"."tb_browse_history" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_browse_history
-- ----------------------------
ALTER TABLE "public"."tb_browse_history" ADD CONSTRAINT "uk_user_article" UNIQUE ("user_id", "article_id");

-- ----------------------------
-- Primary Key structure for table tb_browse_history
-- ----------------------------
ALTER TABLE "public"."tb_browse_history" ADD CONSTRAINT "tb_browse_history_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_category
-- ----------------------------
CREATE INDEX "idx_category_created_at" ON "public"."tb_category" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_category_sort" ON "public"."tb_category" USING btree (
  "sort" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_category
-- ----------------------------
CREATE TRIGGER "update_tb_category_updated_at" BEFORE UPDATE ON "public"."tb_category"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Primary Key structure for table tb_category
-- ----------------------------
ALTER TABLE "public"."tb_category" ADD CONSTRAINT "tb_category_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_collection
-- ----------------------------
CREATE INDEX "idx_collection_article_id" ON "public"."tb_collection" USING btree (
  "article_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_collection_created_at" ON "public"."tb_collection" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_collection_user_id" ON "public"."tb_collection" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_collection
-- ----------------------------
ALTER TABLE "public"."tb_collection" ADD CONSTRAINT "tb_collection_user_article_unique" UNIQUE ("user_id", "article_id");

-- ----------------------------
-- Primary Key structure for table tb_collection
-- ----------------------------
ALTER TABLE "public"."tb_collection" ADD CONSTRAINT "tb_collection_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_column
-- ----------------------------
CREATE INDEX "idx_column_created_at" ON "public"."tb_column" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_column_status" ON "public"."tb_column" USING btree (
  "status" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_column_user_id" ON "public"."tb_column" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_column
-- ----------------------------
CREATE TRIGGER "update_tb_column_updated_at" BEFORE UPDATE ON "public"."tb_column"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Primary Key structure for table tb_column
-- ----------------------------
ALTER TABLE "public"."tb_column" ADD CONSTRAINT "tb_column_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_column_article
-- ----------------------------
CREATE INDEX "idx_column_article_article_id" ON "public"."tb_column_article" USING btree (
  "article_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_column_article_column_id" ON "public"."tb_column_article" USING btree (
  "column_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_column_article
-- ----------------------------
ALTER TABLE "public"."tb_column_article" ADD CONSTRAINT "uk_column_article" UNIQUE ("column_id", "article_id");

-- ----------------------------
-- Primary Key structure for table tb_column_article
-- ----------------------------
ALTER TABLE "public"."tb_column_article" ADD CONSTRAINT "tb_column_article_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_column_subscription
-- ----------------------------
CREATE INDEX "idx_subscription_column_id" ON "public"."tb_column_subscription" USING btree (
  "column_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_subscription_created_at" ON "public"."tb_column_subscription" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_subscription_user_id" ON "public"."tb_column_subscription" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_column_subscription
-- ----------------------------
CREATE TRIGGER "trigger_update_subscription_count" AFTER INSERT OR DELETE ON "public"."tb_column_subscription"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_column_subscription_count"();

-- ----------------------------
-- Uniques structure for table tb_column_subscription
-- ----------------------------
ALTER TABLE "public"."tb_column_subscription" ADD CONSTRAINT "uk_user_column" UNIQUE ("user_id", "column_id");

-- ----------------------------
-- Primary Key structure for table tb_column_subscription
-- ----------------------------
ALTER TABLE "public"."tb_column_subscription" ADD CONSTRAINT "tb_column_subscription_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_comment
-- ----------------------------
CREATE INDEX "idx_comment_article_id" ON "public"."tb_comment" USING btree (
  "article_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_author_id" ON "public"."tb_comment" USING btree (
  "author_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_created_at" ON "public"."tb_comment" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_is_deleted" ON "public"."tb_comment" USING btree (
  "is_deleted" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_parent_id" ON "public"."tb_comment" USING btree (
  "parent_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_reviewer_id" ON "public"."tb_comment" USING btree (
  "reviewer_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_status" ON "public"."tb_comment" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_comment
-- ----------------------------
CREATE TRIGGER "update_tb_comment_updated_at" BEFORE UPDATE ON "public"."tb_comment"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Primary Key structure for table tb_comment
-- ----------------------------
ALTER TABLE "public"."tb_comment" ADD CONSTRAINT "tb_comment_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_comment_like
-- ----------------------------
CREATE INDEX "idx_comment_like_comment_id" ON "public"."tb_comment_like" USING btree (
  "comment_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_like_created_at" ON "public"."tb_comment_like" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_comment_like_user_id" ON "public"."tb_comment_like" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_comment_like
-- ----------------------------
ALTER TABLE "public"."tb_comment_like" ADD CONSTRAINT "uk_comment_like_user_comment" UNIQUE ("user_id", "comment_id");

-- ----------------------------
-- Primary Key structure for table tb_comment_like
-- ----------------------------
ALTER TABLE "public"."tb_comment_like" ADD CONSTRAINT "tb_comment_like_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_conversation
-- ----------------------------
CREATE INDEX "idx_conversation_target_user_id" ON "public"."tb_conversation" USING btree (
  "target_user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_conversation_user_id" ON "public"."tb_conversation" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_conversation_user_target" ON "public"."tb_conversation" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "target_user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_conversation_user_updated" ON "public"."tb_conversation" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "updated_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table tb_conversation
-- ----------------------------
ALTER TABLE "public"."tb_conversation" ADD CONSTRAINT "tb_conversation_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table tb_coupon_template
-- ----------------------------
ALTER TABLE "public"."tb_coupon_template" ADD CONSTRAINT "tb_coupon_template_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_follow
-- ----------------------------
CREATE INDEX "idx_follow_created_at" ON "public"."tb_follow" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_follow_follower_id" ON "public"."tb_follow" USING btree (
  "follower_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_follow_following_id" ON "public"."tb_follow" USING btree (
  "following_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_follow
-- ----------------------------
ALTER TABLE "public"."tb_follow" ADD CONSTRAINT "tb_follow_follower_following_unique" UNIQUE ("follower_id", "following_id");

-- ----------------------------
-- Primary Key structure for table tb_follow
-- ----------------------------
ALTER TABLE "public"."tb_follow" ADD CONSTRAINT "tb_follow_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table tb_knowledge_base
-- ----------------------------
ALTER TABLE "public"."tb_knowledge_base" ADD CONSTRAINT "tb_knowledge_base_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_knowledge_file
-- ----------------------------
CREATE INDEX "idx_knowledge_file_category" ON "public"."tb_knowledge_file" USING btree (
  "category" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_knowledge_file_created" ON "public"."tb_knowledge_file" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_knowledge_file_uploader" ON "public"."tb_knowledge_file" USING btree (
  "uploader_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_knowledge_file
-- ----------------------------
ALTER TABLE "public"."tb_knowledge_file" ADD CONSTRAINT "tb_knowledge_file_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_local_message
-- ----------------------------
CREATE INDEX "idx_local_message_biz" ON "public"."tb_local_message" USING btree (
  "biz_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
) WHERE is_deleted = false;
CREATE INDEX "idx_local_message_biz_user" ON "public"."tb_local_message" USING btree (
  "biz_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_user_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
) WHERE is_deleted = false;
CREATE INDEX "idx_local_message_status_retry" ON "public"."tb_local_message" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST,
  "next_retry_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
) WHERE is_deleted = false;

-- ----------------------------
-- Primary Key structure for table tb_local_message
-- ----------------------------
ALTER TABLE "public"."tb_local_message" ADD CONSTRAINT "tb_local_message_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_message
-- ----------------------------
CREATE INDEX "idx_message_created_at" ON "public"."tb_message" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_message_receiver_id" ON "public"."tb_message" USING btree (
  "receiver_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_message_receiver_unread" ON "public"."tb_message" USING btree (
  "receiver_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "is_read" "pg_catalog"."bool_ops" ASC NULLS LAST
) WHERE is_read = false;
CREATE INDEX "idx_message_sender_id" ON "public"."tb_message" USING btree (
  "sender_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_message_sender_receiver_time" ON "public"."tb_message" USING btree (
  "sender_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "receiver_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table tb_message
-- ----------------------------
ALTER TABLE "public"."tb_message" ADD CONSTRAINT "tb_message_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_mq_error_log
-- ----------------------------
CREATE INDEX "idx_mq_error_log_created_at" ON "public"."tb_mq_error_log" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mq_error_log_status" ON "public"."tb_mq_error_log" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_mq_error_log
-- ----------------------------
ALTER TABLE "public"."tb_mq_error_log" ADD CONSTRAINT "tb_mq_error_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_order
-- ----------------------------
CREATE INDEX "idx_order_expire" ON "public"."tb_order" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST,
  "expire_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
) WHERE is_deleted = false;
CREATE INDEX "idx_order_user_biz" ON "public"."tb_order" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "biz_type" "pg_catalog"."int2_ops" ASC NULLS LAST,
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "uk_order_no" ON "public"."tb_order" USING btree (
  "order_no" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_order
-- ----------------------------
ALTER TABLE "public"."tb_order" ADD CONSTRAINT "tb_order_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_point_log_202606
-- ----------------------------
CREATE INDEX "tb_point_log_202606_created_at_idx" ON "public"."tb_point_log_202606" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202606_type_idx" ON "public"."tb_point_log_202606" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202606_user_id_idx" ON "public"."tb_point_log_202606" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202606_user_id_type_biz_id_idx" ON "public"."tb_point_log_202606" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_point_log_202606
-- ----------------------------
ALTER TABLE "public"."tb_point_log_202606" ADD CONSTRAINT "tb_point_log_202606_pkey" PRIMARY KEY ("id", "created_at");

-- ----------------------------
-- Indexes structure for table tb_point_log_202607
-- ----------------------------
CREATE INDEX "tb_point_log_202607_created_at_idx" ON "public"."tb_point_log_202607" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202607_type_idx" ON "public"."tb_point_log_202607" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202607_user_id_idx" ON "public"."tb_point_log_202607" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202607_user_id_type_biz_id_idx" ON "public"."tb_point_log_202607" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_point_log_202607
-- ----------------------------
ALTER TABLE "public"."tb_point_log_202607" ADD CONSTRAINT "tb_point_log_202607_pkey" PRIMARY KEY ("id", "created_at");

-- ----------------------------
-- Indexes structure for table tb_point_log_202608
-- ----------------------------
CREATE INDEX "tb_point_log_202608_created_at_idx" ON "public"."tb_point_log_202608" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202608_type_idx" ON "public"."tb_point_log_202608" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202608_user_id_idx" ON "public"."tb_point_log_202608" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "tb_point_log_202608_user_id_type_biz_id_idx" ON "public"."tb_point_log_202608" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_point_log_202608
-- ----------------------------
ALTER TABLE "public"."tb_point_log_202608" ADD CONSTRAINT "tb_point_log_202608_pkey" PRIMARY KEY ("id", "created_at");

-- ----------------------------
-- Indexes structure for table tb_point_rank_log
-- ----------------------------
CREATE INDEX "idx_rank_log_month_rank" ON "public"."tb_point_rank_log" USING btree (
  "year_month" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "rank_num" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_rank_log_year_month" ON "public"."tb_point_rank_log" USING btree (
  "year_month" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_point_rank_log
-- ----------------------------
ALTER TABLE "public"."tb_point_rank_log" ADD CONSTRAINT "uk_rank_log_month_user" UNIQUE ("year_month", "user_id");

-- ----------------------------
-- Primary Key structure for table tb_point_rank_log
-- ----------------------------
ALTER TABLE "public"."tb_point_rank_log" ADD CONSTRAINT "pk_point_rank_log" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_research_report
-- ----------------------------
CREATE INDEX "idx_research_report_created" ON "public"."tb_research_report" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_research_report_user" ON "public"."tb_research_report" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "uk_research_report_task" ON "public"."tb_research_report" USING btree (
  "task_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Indexes structure for table tb_research_task
-- ----------------------------
CREATE INDEX "idx_research_task_created" ON "public"."tb_research_task" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_research_task_status" ON "public"."tb_research_task" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_research_task_user" ON "public"."tb_research_task" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_research_task_user_status" ON "public"."tb_research_task" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "uk_research_task_id" ON "public"."tb_research_task" USING btree (
  "task_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Indexes structure for table tb_sensitive_word
-- ----------------------------
CREATE INDEX "idx_sensitive_word_created_at" ON "public"."tb_sensitive_word" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_sensitive_word_status" ON "public"."tb_sensitive_word" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_sensitive_word
-- ----------------------------
CREATE TRIGGER "update_tb_sensitive_word_updated_at" BEFORE UPDATE ON "public"."tb_sensitive_word"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table tb_sensitive_word
-- ----------------------------
ALTER TABLE "public"."tb_sensitive_word" ADD CONSTRAINT "tb_sensitive_word_word_key" UNIQUE ("word");

-- ----------------------------
-- Primary Key structure for table tb_sensitive_word
-- ----------------------------
ALTER TABLE "public"."tb_sensitive_word" ADD CONSTRAINT "tb_sensitive_word_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_sign_record
-- ----------------------------
CREATE INDEX "idx_sign_record_sign_date" ON "public"."tb_sign_record" USING btree (
  "sign_date" "pg_catalog"."date_ops" ASC NULLS LAST
);
CREATE INDEX "idx_sign_record_user_id" ON "public"."tb_sign_record" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_sign_record
-- ----------------------------
ALTER TABLE "public"."tb_sign_record" ADD CONSTRAINT "uk_sign_record_user_date" UNIQUE ("user_id", "sign_date");

-- ----------------------------
-- Primary Key structure for table tb_sign_record
-- ----------------------------
ALTER TABLE "public"."tb_sign_record" ADD CONSTRAINT "pk_sign_record" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_system_config
-- ----------------------------
CREATE INDEX "idx_system_config_key" ON "public"."tb_system_config" USING btree (
  "config_key" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_system_config_type" ON "public"."tb_system_config" USING btree (
  "config_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_system_config
-- ----------------------------
CREATE TRIGGER "update_tb_system_config_updated_at" BEFORE UPDATE ON "public"."tb_system_config"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table tb_system_config
-- ----------------------------
ALTER TABLE "public"."tb_system_config" ADD CONSTRAINT "uk_system_config_key" UNIQUE ("config_key");

-- ----------------------------
-- Primary Key structure for table tb_system_config
-- ----------------------------
ALTER TABLE "public"."tb_system_config" ADD CONSTRAINT "tb_system_config_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_system_notification
-- ----------------------------
CREATE INDEX "idx_system_notification_created_at" ON "public"."tb_system_notification" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_system_notification_is_read" ON "public"."tb_system_notification" USING btree (
  "is_read" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_system_notification_user_id" ON "public"."tb_system_notification" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_system_notification_user_unread" ON "public"."tb_system_notification" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "is_read" "pg_catalog"."bool_ops" ASC NULLS LAST
) WHERE is_read = false;

-- ----------------------------
-- Primary Key structure for table tb_system_notification
-- ----------------------------
ALTER TABLE "public"."tb_system_notification" ADD CONSTRAINT "tb_system_notification_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_tag
-- ----------------------------
CREATE INDEX "idx_tag_created_at" ON "public"."tb_tag" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_tag_use_count" ON "public"."tb_tag" USING btree (
  "use_count" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_tag
-- ----------------------------
ALTER TABLE "public"."tb_tag" ADD CONSTRAINT "tb_tag_name_key" UNIQUE ("name");

-- ----------------------------
-- Primary Key structure for table tb_tag
-- ----------------------------
ALTER TABLE "public"."tb_tag" ADD CONSTRAINT "tb_tag_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_tcc_transaction
-- ----------------------------
CREATE INDEX "idx_tcc_status_created" ON "public"."tb_tcc_transaction" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
) WHERE is_deleted = false AND status::text = 'TRYING'::text;
CREATE UNIQUE INDEX "uk_tcc_xid_biz_type" ON "public"."tb_tcc_transaction" USING btree (
  "xid" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
) WHERE is_deleted = false;

-- ----------------------------
-- Primary Key structure for table tb_tcc_transaction
-- ----------------------------
ALTER TABLE "public"."tb_tcc_transaction" ADD CONSTRAINT "tb_tcc_transaction_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_user
-- ----------------------------
CREATE INDEX "idx_user_created_at" ON "public"."tb_user" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_email" ON "public"."tb_user" USING btree (
  "email" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_phone" ON "public"."tb_user" USING btree (
  "phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_status" ON "public"."tb_user" USING btree (
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_username" ON "public"."tb_user" USING btree (
  "username" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table tb_user
-- ----------------------------
CREATE TRIGGER "update_tb_user_updated_at" BEFORE UPDATE ON "public"."tb_user"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table tb_user
-- ----------------------------
ALTER TABLE "public"."tb_user" ADD CONSTRAINT "tb_user_username_key" UNIQUE ("username");
ALTER TABLE "public"."tb_user" ADD CONSTRAINT "tb_user_email_key" UNIQUE ("email");
ALTER TABLE "public"."tb_user" ADD CONSTRAINT "tb_user_phone_key" UNIQUE ("phone");

-- ----------------------------
-- Primary Key structure for table tb_user
-- ----------------------------
ALTER TABLE "public"."tb_user" ADD CONSTRAINT "tb_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_user_coupon
-- ----------------------------
CREATE INDEX "idx_user_coupon_expire" ON "public"."tb_user_coupon" USING btree (
  "expire_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
) WHERE status = 0;
CREATE INDEX "idx_user_coupon_status" ON "public"."tb_user_coupon" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "status" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_coupon_user_id" ON "public"."tb_user_coupon" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "uk_user_coupon_template" ON "public"."tb_user_coupon" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "coupon_template_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_user_coupon
-- ----------------------------
ALTER TABLE "public"."tb_user_coupon" ADD CONSTRAINT "tb_user_coupon_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_user_point
-- ----------------------------
CREATE INDEX "idx_user_point_user_id" ON "public"."tb_user_point" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table tb_user_point
-- ----------------------------
ALTER TABLE "public"."tb_user_point" ADD CONSTRAINT "uk_user_point_user_id" UNIQUE ("user_id");

-- ----------------------------
-- Primary Key structure for table tb_user_point
-- ----------------------------
ALTER TABLE "public"."tb_user_point" ADD CONSTRAINT "pk_user_point" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_vip_membership
-- ----------------------------
CREATE INDEX "idx_vip_membership_expire" ON "public"."tb_vip_membership" USING btree (
  "vip_level" "pg_catalog"."int2_ops" ASC NULLS LAST,
  "end_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "uk_vip_membership_user" ON "public"."tb_vip_membership" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_vip_membership
-- ----------------------------
ALTER TABLE "public"."tb_vip_membership" ADD CONSTRAINT "tb_vip_membership_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_vip_plan
-- ----------------------------
CREATE UNIQUE INDEX "uk_vip_plan_code" ON "public"."tb_vip_plan" USING btree (
  "plan_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_vip_plan
-- ----------------------------
ALTER TABLE "public"."tb_vip_plan" ADD CONSTRAINT "tb_vip_plan_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_writing_draft
-- ----------------------------
CREATE UNIQUE INDEX "idx_writing_draft_task_id" ON "public"."tb_writing_draft" USING btree (
  "task_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_writing_draft_user_id" ON "public"."tb_writing_draft" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_writing_draft
-- ----------------------------
ALTER TABLE "public"."tb_writing_draft" ADD CONSTRAINT "tb_writing_draft_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_writing_plan
-- ----------------------------
CREATE INDEX "idx_writing_plan_task_id" ON "public"."tb_writing_plan" USING btree (
  "task_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_writing_plan
-- ----------------------------
ALTER TABLE "public"."tb_writing_plan" ADD CONSTRAINT "tb_writing_plan_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_writing_reflection
-- ----------------------------
CREATE UNIQUE INDEX "idx_writing_reflection_task_id" ON "public"."tb_writing_reflection" USING btree (
  "task_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_writing_reflection
-- ----------------------------
ALTER TABLE "public"."tb_writing_reflection" ADD CONSTRAINT "tb_writing_reflection_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_writing_task
-- ----------------------------
CREATE INDEX "idx_writing_task_status" ON "public"."tb_writing_task" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_writing_task_user_id" ON "public"."tb_writing_task" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_writing_task
-- ----------------------------
ALTER TABLE "public"."tb_writing_task" ADD CONSTRAINT "tb_writing_task_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_memories
-- ----------------------------
CREATE INDEX "user_memories_hnsw_idx" ON "public"."user_memories" (
  "vector" "public"."vector_cosine_ops" ASC NULLS LAST
);
CREATE INDEX "user_memories_text_lemmatized_idx" ON "public"."user_memories" USING gin (
  to_tsvector('simple'::regconfig, payload ->> 'text_lemmatized'::text) "pg_catalog"."tsvector_ops"
);

-- ----------------------------
-- Primary Key structure for table user_memories
-- ----------------------------
ALTER TABLE "public"."user_memories" ADD CONSTRAINT "user_memories_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table tb_point_log
-- ----------------------------
CREATE INDEX "idx_point_log_created_at" ON "public"."tb_point_log" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_point_log_type" ON "public"."tb_point_log" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_point_log_user_id" ON "public"."tb_point_log" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_point_log_user_type_biz" ON "public"."tb_point_log" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "biz_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table tb_point_log
-- ----------------------------
ALTER TABLE "public"."tb_point_log" ADD CONSTRAINT "pk_point_log" PRIMARY KEY ("id", "created_at");

-- ----------------------------
-- Foreign Keys structure for table tb_column_subscription
-- ----------------------------
ALTER TABLE "public"."tb_column_subscription" ADD CONSTRAINT "fk_subscription_column" FOREIGN KEY ("column_id") REFERENCES "public"."tb_column" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."tb_column_subscription" ADD CONSTRAINT "fk_subscription_user" FOREIGN KEY ("user_id") REFERENCES "public"."tb_user" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table tb_writing_draft
-- ----------------------------
ALTER TABLE "public"."tb_writing_draft" ADD CONSTRAINT "fk_writing_draft_task" FOREIGN KEY ("task_id") REFERENCES "public"."tb_writing_task" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table tb_writing_reflection
-- ----------------------------
ALTER TABLE "public"."tb_writing_reflection" ADD CONSTRAINT "fk_writing_reflection_task" FOREIGN KEY ("task_id") REFERENCES "public"."tb_writing_task" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
