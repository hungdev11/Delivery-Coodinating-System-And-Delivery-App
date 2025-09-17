-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: server_user
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ADMIN_EVENT_ENTITY`
--

DROP TABLE IF EXISTS `ADMIN_EVENT_ENTITY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ADMIN_EVENT_ENTITY` (
  `ID` varchar(36) NOT NULL,
  `ADMIN_EVENT_TIME` bigint DEFAULT NULL,
  `REALM_ID` varchar(255) DEFAULT NULL,
  `OPERATION_TYPE` varchar(255) DEFAULT NULL,
  `AUTH_REALM_ID` varchar(255) DEFAULT NULL,
  `AUTH_CLIENT_ID` varchar(255) DEFAULT NULL,
  `AUTH_USER_ID` varchar(255) DEFAULT NULL,
  `IP_ADDRESS` varchar(255) DEFAULT NULL,
  `RESOURCE_PATH` text,
  `REPRESENTATION` text,
  `ERROR` varchar(255) DEFAULT NULL,
  `RESOURCE_TYPE` varchar(64) DEFAULT NULL,
  `DETAILS_JSON` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  PRIMARY KEY (`ID`),
  KEY `IDX_ADMIN_EVENT_TIME` (`REALM_ID`,`ADMIN_EVENT_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ADMIN_EVENT_ENTITY`
--

/*!40000 ALTER TABLE `ADMIN_EVENT_ENTITY` DISABLE KEYS */;
INSERT INTO `ADMIN_EVENT_ENTITY` VALUES ('7bbe8b95-d063-4188-8ccc-8b76ba0d1dfe',1755975333950,'b90e8c11-9258-44ee-901a-1650b6e14601','CREATE','b90e8c11-9258-44ee-901a-1650b6e14601','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','dce7ffce-1455-4041-8f58-224b6d426684','172.18.0.4','users/4030193c-c653-4e74-a4b4-6fd4e06fb4cf',NULL,NULL,'USER',NULL),('81a38e90-16d7-469e-b018-764e8799165a',1756031809387,'b90e8c11-9258-44ee-901a-1650b6e14601','CREATE','b90e8c11-9258-44ee-901a-1650b6e14601','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','dce7ffce-1455-4041-8f58-224b6d426684','172.18.0.1','users/c60b0781-0325-4114-bf24-7fcf6ff76cbb',NULL,NULL,'USER',NULL),('9280aee2-f1d6-45b2-821f-5768031581fa',1756030256778,'b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE','736d3e5b-4c46-41ed-9afb-47e727ecab9e','e12fceb4-c28d-4e75-8f26-48fe7824c110','2dac9aff-3c68-43d4-8f18-082789a6c867','172.18.0.1','clients/028d7f5b-9f03-4977-bc22-d7ddad3abbdf',NULL,NULL,'CLIENT',NULL),('c834e065-ec74-43f2-9344-5307d0c94cd7',1756033585413,'b90e8c11-9258-44ee-901a-1650b6e14601','CREATE','736d3e5b-4c46-41ed-9afb-47e727ecab9e','e12fceb4-c28d-4e75-8f26-48fe7824c110','2dac9aff-3c68-43d4-8f18-082789a6c867','172.18.0.1','users/4030193c-c653-4e74-a4b4-6fd4e06fb4cf/role-mappings/clients/028d7f5b-9f03-4977-bc22-d7ddad3abbdf',NULL,NULL,'CLIENT_ROLE_MAPPING',NULL);
/*!40000 ALTER TABLE `ADMIN_EVENT_ENTITY` ENABLE KEYS */;

--
-- Table structure for table `ASSOCIATED_POLICY`
--

DROP TABLE IF EXISTS `ASSOCIATED_POLICY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ASSOCIATED_POLICY` (
  `POLICY_ID` varchar(36) NOT NULL,
  `ASSOCIATED_POLICY_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`POLICY_ID`,`ASSOCIATED_POLICY_ID`),
  KEY `IDX_ASSOC_POL_ASSOC_POL_ID` (`ASSOCIATED_POLICY_ID`),
  CONSTRAINT `FK_FRSR5S213XCX4WNKOG82SSRFY` FOREIGN KEY (`ASSOCIATED_POLICY_ID`) REFERENCES `RESOURCE_SERVER_POLICY` (`ID`),
  CONSTRAINT `FK_FRSRPAS14XCX4WNKOG82SSRFY` FOREIGN KEY (`POLICY_ID`) REFERENCES `RESOURCE_SERVER_POLICY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ASSOCIATED_POLICY`
--

/*!40000 ALTER TABLE `ASSOCIATED_POLICY` DISABLE KEYS */;
/*!40000 ALTER TABLE `ASSOCIATED_POLICY` ENABLE KEYS */;

--
-- Table structure for table `AUTHENTICATION_EXECUTION`
--

DROP TABLE IF EXISTS `AUTHENTICATION_EXECUTION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AUTHENTICATION_EXECUTION` (
  `ID` varchar(36) NOT NULL,
  `ALIAS` varchar(255) DEFAULT NULL,
  `AUTHENTICATOR` varchar(36) DEFAULT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  `FLOW_ID` varchar(36) DEFAULT NULL,
  `REQUIREMENT` int DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `AUTHENTICATOR_FLOW` tinyint NOT NULL DEFAULT '0',
  `AUTH_FLOW_ID` varchar(36) DEFAULT NULL,
  `AUTH_CONFIG` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTH_EXEC_REALM_FLOW` (`REALM_ID`,`FLOW_ID`),
  KEY `IDX_AUTH_EXEC_FLOW` (`FLOW_ID`),
  CONSTRAINT `FK_AUTH_EXEC_FLOW` FOREIGN KEY (`FLOW_ID`) REFERENCES `AUTHENTICATION_FLOW` (`ID`),
  CONSTRAINT `FK_AUTH_EXEC_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `AUTHENTICATION_EXECUTION`
--

/*!40000 ALTER TABLE `AUTHENTICATION_EXECUTION` DISABLE KEYS */;
INSERT INTO `AUTHENTICATION_EXECUTION` VALUES ('01b33cc4-7852-4b12-9f4a-635e9da3bed5',NULL,'conditional-user-configured','b90e8c11-9258-44ee-901a-1650b6e14601','a324f895-bcfd-4845-bce5-a0cbc2dafc6f',0,10,0,NULL,NULL),('03158bd5-ee06-4149-928d-690bbc6da727',NULL,'reset-credentials-choose-user','b90e8c11-9258-44ee-901a-1650b6e14601','e676ea9b-7621-4bed-b24a-42a5fe955f11',0,10,0,NULL,NULL),('0427df1b-8a9a-4418-8e0c-b836540ae8e7',NULL,'client-secret-jwt','b90e8c11-9258-44ee-901a-1650b6e14601','de078332-f55f-4944-9bf4-b6ca668f56ab',2,30,0,NULL,NULL),('05e406c3-c92f-4979-afbf-e9d18422b3c5',NULL,'direct-grant-validate-password','736d3e5b-4c46-41ed-9afb-47e727ecab9e','7a195384-cbf6-4e7d-9c6d-7a3008aeeee0',0,20,0,NULL,NULL),('07c6a381-0bfa-4db3-94f4-5a59c60266d4',NULL,'direct-grant-validate-password','b90e8c11-9258-44ee-901a-1650b6e14601','c75b3bc1-3d7e-4e5c-85bb-0988ffae04e8',0,20,0,NULL,NULL),('08d9b743-2c28-4301-9695-251659210afa',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','e676ea9b-7621-4bed-b24a-42a5fe955f11',1,40,1,'ec8745e5-2762-491d-a217-48295e7a56f4',NULL),('0cf32843-ec17-4a24-b66c-948605c9ce0e',NULL,'auth-cookie','b90e8c11-9258-44ee-901a-1650b6e14601','919e97ab-8fa3-4795-b38b-cef08e92bd01',2,10,0,NULL,NULL),('0d8700dd-d7ce-4da3-8b47-17d22221011c',NULL,'client-x509','b90e8c11-9258-44ee-901a-1650b6e14601','de078332-f55f-4944-9bf4-b6ca668f56ab',2,40,0,NULL,NULL),('151c50b3-248d-4da6-a106-496f9e6d0138',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','612c3b47-8df0-45e8-bb22-42ef53e57d3c',0,20,1,'76621261-c7e8-4e71-8c0c-dcbe2d86fe8c',NULL),('17607f8b-b2ab-4035-8462-6646206ac02c',NULL,'docker-http-basic-authenticator','b90e8c11-9258-44ee-901a-1650b6e14601','67700e74-8e4b-4caf-80b5-8b3bdb8a6b0b',0,10,0,NULL,NULL),('1b711d72-223d-4c99-9f2d-cb4faf429166',NULL,'idp-review-profile','b90e8c11-9258-44ee-901a-1650b6e14601','9bc9ebf8-8728-40df-92f4-baa7e060d6b6',0,10,0,NULL,'b9abe0ae-c7bb-4e6f-892c-1c6da60cdc90'),('1cd7c33a-b886-408a-af54-d0733890f62e',NULL,'auth-spnego','736d3e5b-4c46-41ed-9afb-47e727ecab9e','4e3a7162-5fe3-42a5-9ddb-3f1326baa7e9',3,20,0,NULL,NULL),('1cdd7579-a032-4ecc-ac50-1bbb840545bd',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','7a195384-cbf6-4e7d-9c6d-7a3008aeeee0',1,30,1,'704fb7b7-001f-44e7-aa3a-f82e0227c3e6',NULL),('1f580f99-b225-4fc6-b516-afd3482cd657',NULL,'registration-recaptcha-action','736d3e5b-4c46-41ed-9afb-47e727ecab9e','0b65f832-f2de-4c6b-ad45-9e1a9832e2fe',3,60,0,NULL,NULL),('212f9da0-fd67-4e86-bbaa-8609550469ba',NULL,'conditional-user-configured','b90e8c11-9258-44ee-901a-1650b6e14601','d9e13dfc-3bb1-4427-8bb8-672f83110c9f',0,10,0,NULL,NULL),('22762a70-9019-42fa-9a7d-ed5d6b2c8489',NULL,'idp-confirm-link','736d3e5b-4c46-41ed-9afb-47e727ecab9e','58cc14b5-f759-43d1-be85-f1d4da8fd161',0,10,0,NULL,NULL),('22ab7218-309b-4f83-969a-90bad5498588',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','da7a3030-800c-4d7d-9768-29f323fd9448',2,20,1,'5417dd6a-d0c7-4e1e-9b06-4f22deee4bf3',NULL),('23ccd0de-ab48-42bb-bb8b-0a72a8321791',NULL,'idp-username-password-form','736d3e5b-4c46-41ed-9afb-47e727ecab9e','5417dd6a-d0c7-4e1e-9b06-4f22deee4bf3',0,10,0,NULL,NULL),('25076b28-a9a5-4e3c-9da3-f437c90deb7d',NULL,'auth-spnego','b90e8c11-9258-44ee-901a-1650b6e14601','919e97ab-8fa3-4795-b38b-cef08e92bd01',3,20,0,NULL,NULL),('2914166f-d5d0-47c8-acbf-93cfc4aa3c32',NULL,'conditional-user-configured','736d3e5b-4c46-41ed-9afb-47e727ecab9e','f52629f6-592d-4ab5-864c-086801b98795',0,10,0,NULL,NULL),('2c876fb7-f317-4d4a-9a03-e9919ea8f143',NULL,'idp-review-profile','736d3e5b-4c46-41ed-9afb-47e727ecab9e','612c3b47-8df0-45e8-bb22-42ef53e57d3c',0,10,0,NULL,'e43b8da0-8bf7-44df-bae9-32634b91e7c1'),('2d96fe6c-28e1-4201-9e0f-f6b76de9c77e',NULL,'http-basic-authenticator','736d3e5b-4c46-41ed-9afb-47e727ecab9e','67403d6c-3ab2-4abe-b366-834132891b0e',0,10,0,NULL,NULL),('2dbca6f6-701e-40af-a9bd-071e541e26cf',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','90196371-9ad5-4226-a383-6e87e925758d',2,20,1,'05bc898d-955a-4f02-a5d9-2c176e7d4a02',NULL),('2e689bab-d3d9-4312-9246-48981d5a8cd9',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','58cc14b5-f759-43d1-be85-f1d4da8fd161',0,20,1,'da7a3030-800c-4d7d-9768-29f323fd9448',NULL),('2f491ee9-dad5-47e7-81b2-3d150571da3f',NULL,'idp-confirm-link','b90e8c11-9258-44ee-901a-1650b6e14601','f51b7ad3-9316-4101-bc6c-b28513ae9c2f',0,10,0,NULL,NULL),('33d90b0e-e049-4839-aab7-c6d346d9f6e4',NULL,'idp-create-user-if-unique','b90e8c11-9258-44ee-901a-1650b6e14601','48411980-5544-42e5-9053-32eaadaada40',2,10,0,NULL,'ea6f7a8d-2298-4ecd-b41a-476005b836b4'),('3750274d-e0d0-41de-9b99-e49ecfc20bd0',NULL,'reset-credential-email','736d3e5b-4c46-41ed-9afb-47e727ecab9e','701651b2-d532-4922-8afa-6d93114d85b4',0,20,0,NULL,NULL),('3a697389-0435-4837-8d36-172a6c98b7ec',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','919e97ab-8fa3-4795-b38b-cef08e92bd01',2,26,1,'a8569d55-4ce3-4a1e-b53a-4b0f21d831d0',NULL),('424005ca-0172-4338-8f28-48fab4432c84',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','919e97ab-8fa3-4795-b38b-cef08e92bd01',2,30,1,'f530dc1e-75a1-4462-b119-b02b62e5389d',NULL),('437a4cf0-3da4-4ac9-9c7f-fd54a6b651f5',NULL,'conditional-user-configured','736d3e5b-4c46-41ed-9afb-47e727ecab9e','704fb7b7-001f-44e7-aa3a-f82e0227c3e6',0,10,0,NULL,NULL),('45d42303-fa6c-4ccd-a93e-86a2992eece4',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','05bc898d-955a-4f02-a5d9-2c176e7d4a02',1,20,1,'9b7a9d81-a2ff-4285-b90c-48f8909be006',NULL),('467938b6-c401-4214-9ccd-482a2d127f95',NULL,'registration-page-form','b90e8c11-9258-44ee-901a-1650b6e14601','052cc613-6116-46a5-b2cd-a8d5f1b8eeb0',0,10,1,'7279a357-64fe-4bf4-8ea8-cb007e3e4ebc',NULL),('4b2b0874-716c-4ca4-bbc3-51c72918be07',NULL,'reset-otp','b90e8c11-9258-44ee-901a-1650b6e14601','ec8745e5-2762-491d-a217-48295e7a56f4',0,20,0,NULL,NULL),('552974cd-8ccb-4623-9cf7-549eca8900fb',NULL,'auth-otp-form','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3184f705-2621-4155-9625-b64b7d615185',0,20,0,NULL,NULL),('55a3584b-5ca2-441c-adbd-7d4a6870d6c1',NULL,'client-jwt','b90e8c11-9258-44ee-901a-1650b6e14601','de078332-f55f-4944-9bf4-b6ca668f56ab',2,20,0,NULL,NULL),('55ef1571-bb44-4570-a2a0-67eb8cc7d9c6',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','f530dc1e-75a1-4462-b119-b02b62e5389d',1,20,1,'71ae40f8-212e-488c-b1f2-55178d67f30a',NULL),('58317133-d4d9-464a-abec-9cbadf8e3364',NULL,'registration-password-action','b90e8c11-9258-44ee-901a-1650b6e14601','7279a357-64fe-4bf4-8ea8-cb007e3e4ebc',0,50,0,NULL,NULL),('68325b46-fb53-417e-8042-694bff933be0',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','c75b3bc1-3d7e-4e5c-85bb-0988ffae04e8',1,30,1,'a324f895-bcfd-4845-bce5-a0cbc2dafc6f',NULL),('68f268f3-cc2e-42e4-948e-f0287c4886a6',NULL,'idp-email-verification','736d3e5b-4c46-41ed-9afb-47e727ecab9e','da7a3030-800c-4d7d-9768-29f323fd9448',2,10,0,NULL,NULL),('6fb8d4c2-9807-4dc4-a1fc-0f1aa629ab88',NULL,'identity-provider-redirector','b90e8c11-9258-44ee-901a-1650b6e14601','919e97ab-8fa3-4795-b38b-cef08e92bd01',2,25,0,NULL,NULL),('6fcfa9c6-d209-44a0-9f5c-03b1efaf413e',NULL,'direct-grant-validate-otp','b90e8c11-9258-44ee-901a-1650b6e14601','a324f895-bcfd-4845-bce5-a0cbc2dafc6f',0,20,0,NULL,NULL),('7794e7f2-102c-4f16-923f-02b897fce3c3',NULL,'reset-password','b90e8c11-9258-44ee-901a-1650b6e14601','e676ea9b-7621-4bed-b24a-42a5fe955f11',0,30,0,NULL,NULL),('77e312f1-42b4-4e04-b957-0e0180c56463',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','4e3a7162-5fe3-42a5-9ddb-3f1326baa7e9',2,30,1,'ffc3aa86-3df8-4d56-81d2-9f5876d31c90',NULL),('7830fa4c-9f6c-4766-883b-7085ce17e680',NULL,'registration-page-form','736d3e5b-4c46-41ed-9afb-47e727ecab9e','14fc0faa-47cb-4e83-b11a-e5dd766fe5ce',0,10,1,'0b65f832-f2de-4c6b-ad45-9e1a9832e2fe',NULL),('7e3854dc-63fb-4045-93bf-ff097986f908',NULL,'direct-grant-validate-username','736d3e5b-4c46-41ed-9afb-47e727ecab9e','7a195384-cbf6-4e7d-9c6d-7a3008aeeee0',0,10,0,NULL,NULL),('7f115f80-da5d-45e6-b7b6-c5a83aec3699',NULL,'conditional-user-configured','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3184f705-2621-4155-9625-b64b7d615185',0,10,0,NULL,NULL),('804db2ef-afc9-44bd-86ec-437b0eb83765',NULL,'client-x509','736d3e5b-4c46-41ed-9afb-47e727ecab9e','bc21a24a-d001-4d96-aaf8-062112ed1e2b',2,40,0,NULL,NULL),('831a106e-b15c-4798-a465-0a7c66ba9303',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','9bc9ebf8-8728-40df-92f4-baa7e060d6b6',0,20,1,'48411980-5544-42e5-9053-32eaadaada40',NULL),('88e21651-ce90-437b-a9ae-88b076dfad0b',NULL,'registration-password-action','736d3e5b-4c46-41ed-9afb-47e727ecab9e','0b65f832-f2de-4c6b-ad45-9e1a9832e2fe',0,50,0,NULL,NULL),('89dd5832-b122-4663-afdb-3bab61399f5c',NULL,'client-secret-jwt','736d3e5b-4c46-41ed-9afb-47e727ecab9e','bc21a24a-d001-4d96-aaf8-062112ed1e2b',2,30,0,NULL,NULL),('8cf169e6-0b5a-4c92-86d7-9b1a51952056',NULL,'idp-email-verification','b90e8c11-9258-44ee-901a-1650b6e14601','90196371-9ad5-4226-a383-6e87e925758d',2,10,0,NULL,NULL),('8dfe0759-90da-45a7-ab26-4bb86f810e69',NULL,'idp-add-organization-member','b90e8c11-9258-44ee-901a-1650b6e14601','d9e13dfc-3bb1-4427-8bb8-672f83110c9f',0,20,0,NULL,NULL),('91d80ba2-88a7-4cbc-a299-0339795278ab',NULL,'registration-user-creation','736d3e5b-4c46-41ed-9afb-47e727ecab9e','0b65f832-f2de-4c6b-ad45-9e1a9832e2fe',0,20,0,NULL,NULL),('94a86d2b-6803-4e47-8da2-88bc111fc864',NULL,'reset-credential-email','b90e8c11-9258-44ee-901a-1650b6e14601','e676ea9b-7621-4bed-b24a-42a5fe955f11',0,20,0,NULL,NULL),('9c3d85ec-a001-4233-83ec-b74e5bd31cbd',NULL,'auth-username-password-form','736d3e5b-4c46-41ed-9afb-47e727ecab9e','ffc3aa86-3df8-4d56-81d2-9f5876d31c90',0,10,0,NULL,NULL),('9c4076fc-7ad4-4feb-a41f-091e45ce4d7b',NULL,'idp-username-password-form','b90e8c11-9258-44ee-901a-1650b6e14601','05bc898d-955a-4f02-a5d9-2c176e7d4a02',0,10,0,NULL,NULL),('9f42f2ce-f3fc-494f-8937-2112911a0e5c',NULL,'conditional-user-configured','b90e8c11-9258-44ee-901a-1650b6e14601','b0d2c3e2-d345-4f99-8bca-a17560de7e71',0,10,0,NULL,NULL),('a34ac217-b71c-44f0-abdb-82c6ceb182a8',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','48411980-5544-42e5-9053-32eaadaada40',2,20,1,'f51b7ad3-9316-4101-bc6c-b28513ae9c2f',NULL),('a5849cbf-558a-4b47-94de-881090eb7a14',NULL,'registration-terms-and-conditions','b90e8c11-9258-44ee-901a-1650b6e14601','7279a357-64fe-4bf4-8ea8-cb007e3e4ebc',3,70,0,NULL,NULL),('a6400344-054e-47ef-b266-358179778431',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','f51b7ad3-9316-4101-bc6c-b28513ae9c2f',0,20,1,'90196371-9ad5-4226-a383-6e87e925758d',NULL),('a74d079b-b6d0-4e8d-9af4-fadb50bb4aba',NULL,'http-basic-authenticator','b90e8c11-9258-44ee-901a-1650b6e14601','f6da23ea-091d-4528-8f99-757fc8074393',0,10,0,NULL,NULL),('a8cb6b94-e173-4890-8486-1875b2ef86d0',NULL,'organization','b90e8c11-9258-44ee-901a-1650b6e14601','b0d2c3e2-d345-4f99-8bca-a17560de7e71',2,20,0,NULL,NULL),('acb676e4-2511-43e0-9a24-3081958eee8b',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','701651b2-d532-4922-8afa-6d93114d85b4',1,40,1,'a1a46e81-e921-417a-8498-2eca8c4fb092',NULL),('b5209434-fe7a-42e6-bfff-7a6cba5b364a',NULL,'auth-otp-form','b90e8c11-9258-44ee-901a-1650b6e14601','71ae40f8-212e-488c-b1f2-55178d67f30a',0,20,0,NULL,NULL),('ba48bd96-827c-46e5-8860-483a41bffb76',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','a8569d55-4ce3-4a1e-b53a-4b0f21d831d0',1,10,1,'b0d2c3e2-d345-4f99-8bca-a17560de7e71',NULL),('bedb1650-2a78-451e-b024-8183a235e28d',NULL,'client-jwt','736d3e5b-4c46-41ed-9afb-47e727ecab9e','bc21a24a-d001-4d96-aaf8-062112ed1e2b',2,20,0,NULL,NULL),('c06a3b44-876b-49e8-acba-da41c3407452',NULL,'client-secret','b90e8c11-9258-44ee-901a-1650b6e14601','de078332-f55f-4944-9bf4-b6ca668f56ab',2,10,0,NULL,NULL),('c1f94b6a-2058-484e-9435-1007fc475359',NULL,'auth-cookie','736d3e5b-4c46-41ed-9afb-47e727ecab9e','4e3a7162-5fe3-42a5-9ddb-3f1326baa7e9',2,10,0,NULL,NULL),('c28423c0-8f48-406c-9b3b-62f7aaa7df0f',NULL,'reset-credentials-choose-user','736d3e5b-4c46-41ed-9afb-47e727ecab9e','701651b2-d532-4922-8afa-6d93114d85b4',0,10,0,NULL,NULL),('c2e459a7-8618-4069-8429-22fd3eb52a75',NULL,'direct-grant-validate-otp','736d3e5b-4c46-41ed-9afb-47e727ecab9e','704fb7b7-001f-44e7-aa3a-f82e0227c3e6',0,20,0,NULL,NULL),('c48cda1e-d1ca-440b-ad05-829f35821b8f',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','5417dd6a-d0c7-4e1e-9b06-4f22deee4bf3',1,20,1,'f52629f6-592d-4ab5-864c-086801b98795',NULL),('c4fada70-fee2-492f-a2a9-b7b69b1a2b6a',NULL,'direct-grant-validate-username','b90e8c11-9258-44ee-901a-1650b6e14601','c75b3bc1-3d7e-4e5c-85bb-0988ffae04e8',0,10,0,NULL,NULL),('c51b3fa4-d861-4701-98b5-a83049c044e8',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','ffc3aa86-3df8-4d56-81d2-9f5876d31c90',1,20,1,'3184f705-2621-4155-9625-b64b7d615185',NULL),('ca3e2da3-01ed-448d-a83c-b9a4b97a01af',NULL,'client-secret','736d3e5b-4c46-41ed-9afb-47e727ecab9e','bc21a24a-d001-4d96-aaf8-062112ed1e2b',2,10,0,NULL,NULL),('ca87cbc2-5c5a-43be-bacd-2a9154b517bd',NULL,'registration-terms-and-conditions','736d3e5b-4c46-41ed-9afb-47e727ecab9e','0b65f832-f2de-4c6b-ad45-9e1a9832e2fe',3,70,0,NULL,NULL),('cc15503a-ac9e-4b48-b07c-354701be27c3',NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','9bc9ebf8-8728-40df-92f4-baa7e060d6b6',1,50,1,'d9e13dfc-3bb1-4427-8bb8-672f83110c9f',NULL),('ce903118-60b4-4b6b-9502-2b17ff808447',NULL,'idp-create-user-if-unique','736d3e5b-4c46-41ed-9afb-47e727ecab9e','76621261-c7e8-4e71-8c0c-dcbe2d86fe8c',2,10,0,NULL,'dc73a340-97c5-44e4-aa94-8c421f2fe710'),('cfdadd2f-f1fb-42af-ba9c-d25e339412a5',NULL,'conditional-user-configured','b90e8c11-9258-44ee-901a-1650b6e14601','71ae40f8-212e-488c-b1f2-55178d67f30a',0,10,0,NULL,NULL),('d05827b9-3ddf-4179-9387-bea239fb57f9',NULL,'conditional-user-configured','b90e8c11-9258-44ee-901a-1650b6e14601','9b7a9d81-a2ff-4285-b90c-48f8909be006',0,10,0,NULL,NULL),('d472824c-55b0-4aad-8b4c-5c3b1973351e',NULL,'registration-user-creation','b90e8c11-9258-44ee-901a-1650b6e14601','7279a357-64fe-4bf4-8ea8-cb007e3e4ebc',0,20,0,NULL,NULL),('d77e2bbe-d0cc-4ae4-8769-8ea83cb8d2bf',NULL,'auth-otp-form','736d3e5b-4c46-41ed-9afb-47e727ecab9e','f52629f6-592d-4ab5-864c-086801b98795',0,20,0,NULL,NULL),('dba20a97-a4b3-4f96-9535-e1edea9ab92e',NULL,'conditional-user-configured','b90e8c11-9258-44ee-901a-1650b6e14601','ec8745e5-2762-491d-a217-48295e7a56f4',0,10,0,NULL,NULL),('de1531c0-6c4f-4755-b703-6e297a83bb71',NULL,'registration-recaptcha-action','b90e8c11-9258-44ee-901a-1650b6e14601','7279a357-64fe-4bf4-8ea8-cb007e3e4ebc',3,60,0,NULL,NULL),('e26f1542-586a-47ce-899d-198d579e7e92',NULL,'reset-otp','736d3e5b-4c46-41ed-9afb-47e727ecab9e','a1a46e81-e921-417a-8498-2eca8c4fb092',0,20,0,NULL,NULL),('e305d251-3e86-4202-b5f7-6f76ebac9c1e',NULL,'identity-provider-redirector','736d3e5b-4c46-41ed-9afb-47e727ecab9e','4e3a7162-5fe3-42a5-9ddb-3f1326baa7e9',2,25,0,NULL,NULL),('e6b4a97c-ada0-4db4-b325-919c8096a272',NULL,'reset-password','736d3e5b-4c46-41ed-9afb-47e727ecab9e','701651b2-d532-4922-8afa-6d93114d85b4',0,30,0,NULL,NULL),('ecac01ee-b795-4eec-8d05-8bae3473c3a3',NULL,'conditional-user-configured','736d3e5b-4c46-41ed-9afb-47e727ecab9e','a1a46e81-e921-417a-8498-2eca8c4fb092',0,10,0,NULL,NULL),('f031735b-0f4a-4944-8b76-cb88defeeab0',NULL,'auth-otp-form','b90e8c11-9258-44ee-901a-1650b6e14601','9b7a9d81-a2ff-4285-b90c-48f8909be006',0,20,0,NULL,NULL),('f1b6d969-27dc-4d30-9757-187b0311e6f8',NULL,'docker-http-basic-authenticator','736d3e5b-4c46-41ed-9afb-47e727ecab9e','dc2bb502-e5df-4e86-b135-7810a3780ab2',0,10,0,NULL,NULL),('f2913619-b20e-4734-a73c-eb86e29d1f2a',NULL,'auth-username-password-form','b90e8c11-9258-44ee-901a-1650b6e14601','f530dc1e-75a1-4462-b119-b02b62e5389d',0,10,0,NULL,NULL),('fe30cd30-53de-450a-9d6a-8cceba3564f1',NULL,NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','76621261-c7e8-4e71-8c0c-dcbe2d86fe8c',2,20,1,'58cc14b5-f759-43d1-be85-f1d4da8fd161',NULL);
/*!40000 ALTER TABLE `AUTHENTICATION_EXECUTION` ENABLE KEYS */;

--
-- Table structure for table `AUTHENTICATION_FLOW`
--

DROP TABLE IF EXISTS `AUTHENTICATION_FLOW`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AUTHENTICATION_FLOW` (
  `ID` varchar(36) NOT NULL,
  `ALIAS` varchar(255) DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  `PROVIDER_ID` varchar(36) NOT NULL DEFAULT 'basic-flow',
  `TOP_LEVEL` tinyint NOT NULL DEFAULT '0',
  `BUILT_IN` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTH_FLOW_REALM` (`REALM_ID`),
  CONSTRAINT `FK_AUTH_FLOW_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `AUTHENTICATION_FLOW`
--

/*!40000 ALTER TABLE `AUTHENTICATION_FLOW` DISABLE KEYS */;
INSERT INTO `AUTHENTICATION_FLOW` VALUES ('052cc613-6116-46a5-b2cd-a8d5f1b8eeb0','registration','Registration flow','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',1,1),('05bc898d-955a-4f02-a5d9-2c176e7d4a02','Verify Existing Account by Re-authentication','Reauthentication of existing account','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('0b65f832-f2de-4c6b-ad45-9e1a9832e2fe','registration form','Registration form','736d3e5b-4c46-41ed-9afb-47e727ecab9e','form-flow',0,1),('14fc0faa-47cb-4e83-b11a-e5dd766fe5ce','registration','Registration flow','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',1,1),('3184f705-2621-4155-9625-b64b7d615185','Browser - Conditional OTP','Flow to determine if the OTP is required for the authentication','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('48411980-5544-42e5-9053-32eaadaada40','User creation or linking','Flow for the existing/non-existing user alternatives','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('4e3a7162-5fe3-42a5-9ddb-3f1326baa7e9','browser','Browser based authentication','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',1,1),('5417dd6a-d0c7-4e1e-9b06-4f22deee4bf3','Verify Existing Account by Re-authentication','Reauthentication of existing account','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('58cc14b5-f759-43d1-be85-f1d4da8fd161','Handle Existing Account','Handle what to do if there is existing account with same email/username like authenticated identity provider','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('612c3b47-8df0-45e8-bb22-42ef53e57d3c','first broker login','Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',1,1),('67403d6c-3ab2-4abe-b366-834132891b0e','saml ecp','SAML ECP Profile Authentication Flow','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',1,1),('67700e74-8e4b-4caf-80b5-8b3bdb8a6b0b','docker auth','Used by Docker clients to authenticate against the IDP','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',1,1),('701651b2-d532-4922-8afa-6d93114d85b4','reset credentials','Reset credentials for a user if they forgot their password or something','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',1,1),('704fb7b7-001f-44e7-aa3a-f82e0227c3e6','Direct Grant - Conditional OTP','Flow to determine if the OTP is required for the authentication','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('71ae40f8-212e-488c-b1f2-55178d67f30a','Browser - Conditional OTP','Flow to determine if the OTP is required for the authentication','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('7279a357-64fe-4bf4-8ea8-cb007e3e4ebc','registration form','Registration form','b90e8c11-9258-44ee-901a-1650b6e14601','form-flow',0,1),('76621261-c7e8-4e71-8c0c-dcbe2d86fe8c','User creation or linking','Flow for the existing/non-existing user alternatives','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('7a195384-cbf6-4e7d-9c6d-7a3008aeeee0','direct grant','OpenID Connect Resource Owner Grant','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',1,1),('90196371-9ad5-4226-a383-6e87e925758d','Account verification options','Method with which to verity the existing account','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('919e97ab-8fa3-4795-b38b-cef08e92bd01','browser','Browser based authentication','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',1,1),('9b7a9d81-a2ff-4285-b90c-48f8909be006','First broker login - Conditional OTP','Flow to determine if the OTP is required for the authentication','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('9bc9ebf8-8728-40df-92f4-baa7e060d6b6','first broker login','Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',1,1),('a1a46e81-e921-417a-8498-2eca8c4fb092','Reset - Conditional OTP','Flow to determine if the OTP should be reset or not. Set to REQUIRED to force.','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('a324f895-bcfd-4845-bce5-a0cbc2dafc6f','Direct Grant - Conditional OTP','Flow to determine if the OTP is required for the authentication','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('a8569d55-4ce3-4a1e-b53a-4b0f21d831d0','Organization',NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('b0d2c3e2-d345-4f99-8bca-a17560de7e71','Browser - Conditional Organization','Flow to determine if the organization identity-first login is to be used','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('bc21a24a-d001-4d96-aaf8-062112ed1e2b','clients','Base authentication for clients','736d3e5b-4c46-41ed-9afb-47e727ecab9e','client-flow',1,1),('c75b3bc1-3d7e-4e5c-85bb-0988ffae04e8','direct grant','OpenID Connect Resource Owner Grant','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',1,1),('d9e13dfc-3bb1-4427-8bb8-672f83110c9f','First Broker Login - Conditional Organization','Flow to determine if the authenticator that adds organization members is to be used','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('da7a3030-800c-4d7d-9768-29f323fd9448','Account verification options','Method with which to verity the existing account','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('dc2bb502-e5df-4e86-b135-7810a3780ab2','docker auth','Used by Docker clients to authenticate against the IDP','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',1,1),('de078332-f55f-4944-9bf4-b6ca668f56ab','clients','Base authentication for clients','b90e8c11-9258-44ee-901a-1650b6e14601','client-flow',1,1),('e676ea9b-7621-4bed-b24a-42a5fe955f11','reset credentials','Reset credentials for a user if they forgot their password or something','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',1,1),('ec8745e5-2762-491d-a217-48295e7a56f4','Reset - Conditional OTP','Flow to determine if the OTP should be reset or not. Set to REQUIRED to force.','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('f51b7ad3-9316-4101-bc6c-b28513ae9c2f','Handle Existing Account','Handle what to do if there is existing account with same email/username like authenticated identity provider','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('f52629f6-592d-4ab5-864c-086801b98795','First broker login - Conditional OTP','Flow to determine if the OTP is required for the authentication','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1),('f530dc1e-75a1-4462-b119-b02b62e5389d','forms','Username, password, otp and other auth forms.','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',0,1),('f6da23ea-091d-4528-8f99-757fc8074393','saml ecp','SAML ECP Profile Authentication Flow','b90e8c11-9258-44ee-901a-1650b6e14601','basic-flow',1,1),('ffc3aa86-3df8-4d56-81d2-9f5876d31c90','forms','Username, password, otp and other auth forms.','736d3e5b-4c46-41ed-9afb-47e727ecab9e','basic-flow',0,1);
/*!40000 ALTER TABLE `AUTHENTICATION_FLOW` ENABLE KEYS */;

--
-- Table structure for table `AUTHENTICATOR_CONFIG`
--

DROP TABLE IF EXISTS `AUTHENTICATOR_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AUTHENTICATOR_CONFIG` (
  `ID` varchar(36) NOT NULL,
  `ALIAS` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_AUTH_CONFIG_REALM` (`REALM_ID`),
  CONSTRAINT `FK_AUTH_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `AUTHENTICATOR_CONFIG`
--

/*!40000 ALTER TABLE `AUTHENTICATOR_CONFIG` DISABLE KEYS */;
INSERT INTO `AUTHENTICATOR_CONFIG` VALUES ('b9abe0ae-c7bb-4e6f-892c-1c6da60cdc90','review profile config','b90e8c11-9258-44ee-901a-1650b6e14601'),('dc73a340-97c5-44e4-aa94-8c421f2fe710','create unique user config','736d3e5b-4c46-41ed-9afb-47e727ecab9e'),('e43b8da0-8bf7-44df-bae9-32634b91e7c1','review profile config','736d3e5b-4c46-41ed-9afb-47e727ecab9e'),('ea6f7a8d-2298-4ecd-b41a-476005b836b4','create unique user config','b90e8c11-9258-44ee-901a-1650b6e14601');
/*!40000 ALTER TABLE `AUTHENTICATOR_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `AUTHENTICATOR_CONFIG_ENTRY`
--

DROP TABLE IF EXISTS `AUTHENTICATOR_CONFIG_ENTRY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AUTHENTICATOR_CONFIG_ENTRY` (
  `AUTHENTICATOR_ID` varchar(36) NOT NULL,
  `VALUE` longtext,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`AUTHENTICATOR_ID`,`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `AUTHENTICATOR_CONFIG_ENTRY`
--

/*!40000 ALTER TABLE `AUTHENTICATOR_CONFIG_ENTRY` DISABLE KEYS */;
INSERT INTO `AUTHENTICATOR_CONFIG_ENTRY` VALUES ('b9abe0ae-c7bb-4e6f-892c-1c6da60cdc90','missing','update.profile.on.first.login'),('dc73a340-97c5-44e4-aa94-8c421f2fe710','false','require.password.update.after.registration'),('e43b8da0-8bf7-44df-bae9-32634b91e7c1','missing','update.profile.on.first.login'),('ea6f7a8d-2298-4ecd-b41a-476005b836b4','false','require.password.update.after.registration');
/*!40000 ALTER TABLE `AUTHENTICATOR_CONFIG_ENTRY` ENABLE KEYS */;

--
-- Table structure for table `BROKER_LINK`
--

DROP TABLE IF EXISTS `BROKER_LINK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BROKER_LINK` (
  `IDENTITY_PROVIDER` varchar(255) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `BROKER_USER_ID` varchar(255) DEFAULT NULL,
  `BROKER_USERNAME` varchar(255) DEFAULT NULL,
  `TOKEN` text,
  `USER_ID` varchar(255) NOT NULL,
  PRIMARY KEY (`IDENTITY_PROVIDER`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `BROKER_LINK`
--

/*!40000 ALTER TABLE `BROKER_LINK` DISABLE KEYS */;
/*!40000 ALTER TABLE `BROKER_LINK` ENABLE KEYS */;

--
-- Table structure for table `CLIENT`
--

DROP TABLE IF EXISTS `CLIENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT` (
  `ID` varchar(36) NOT NULL,
  `ENABLED` tinyint NOT NULL DEFAULT '0',
  `FULL_SCOPE_ALLOWED` tinyint NOT NULL DEFAULT '0',
  `CLIENT_ID` varchar(255) DEFAULT NULL,
  `NOT_BEFORE` int DEFAULT NULL,
  `PUBLIC_CLIENT` tinyint NOT NULL DEFAULT '0',
  `SECRET` varchar(255) DEFAULT NULL,
  `BASE_URL` varchar(255) DEFAULT NULL,
  `BEARER_ONLY` tinyint NOT NULL DEFAULT '0',
  `MANAGEMENT_URL` varchar(255) DEFAULT NULL,
  `SURROGATE_AUTH_REQUIRED` tinyint NOT NULL DEFAULT '0',
  `REALM_ID` varchar(36) DEFAULT NULL,
  `PROTOCOL` varchar(255) DEFAULT NULL,
  `NODE_REREG_TIMEOUT` int DEFAULT '0',
  `FRONTCHANNEL_LOGOUT` tinyint NOT NULL DEFAULT '0',
  `CONSENT_REQUIRED` tinyint NOT NULL DEFAULT '0',
  `NAME` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `SERVICE_ACCOUNTS_ENABLED` tinyint NOT NULL DEFAULT '0',
  `CLIENT_AUTHENTICATOR_TYPE` varchar(255) DEFAULT NULL,
  `ROOT_URL` varchar(255) DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `REGISTRATION_TOKEN` varchar(255) DEFAULT NULL,
  `STANDARD_FLOW_ENABLED` tinyint NOT NULL DEFAULT '1',
  `IMPLICIT_FLOW_ENABLED` tinyint NOT NULL DEFAULT '0',
  `DIRECT_ACCESS_GRANTS_ENABLED` tinyint NOT NULL DEFAULT '0',
  `ALWAYS_DISPLAY_IN_CONSOLE` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_B71CJLBENV945RB6GCON438AT` (`REALM_ID`,`CLIENT_ID`),
  KEY `IDX_CLIENT_ID` (`CLIENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT`
--

/*!40000 ALTER TABLE `CLIENT` DISABLE KEYS */;
INSERT INTO `CLIENT` VALUES ('028d7f5b-9f03-4977-bc22-d7ddad3abbdf',1,1,'doantotnghiep',0,0,'46sGOyba6nyt8UhLkKAQgzmbedF9L042','http://127.0.0.1:5173',0,'http://127.0.0.1:8080/keycloak',0,'b90e8c11-9258-44ee-901a-1650b6e14601','openid-connect',-1,1,0,'doantotnghiep',1,'client-secret','http://127.0.0.1:8080/keycloak','','e1024f9d-cb39-404f-ab4f-0136838bcb6d',1,1,1,0),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,0,'realm-management',0,0,NULL,NULL,1,NULL,0,'b90e8c11-9258-44ee-901a-1650b6e14601','openid-connect',0,0,0,'${client_realm-management}',0,'client-secret',NULL,NULL,NULL,1,0,0,0),('36d6476a-889e-4395-bf12-9fc79c217c36',1,0,'keycloak-realm',0,0,NULL,NULL,1,NULL,0,'736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL,0,0,0,'keycloak Realm',0,'client-secret',NULL,NULL,NULL,1,0,0,0),('3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,0,'master-realm',0,0,NULL,NULL,1,NULL,0,'736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL,0,0,0,'master Realm',0,'client-secret',NULL,NULL,NULL,1,0,0,0),('540a1782-e266-4d93-8898-fd85e3ca4825',1,0,'broker',0,0,NULL,NULL,1,NULL,0,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','openid-connect',0,0,0,'${client_broker}',0,'client-secret',NULL,NULL,NULL,1,0,0,0),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,0,'account',0,1,NULL,'/realms/keycloak/account/',0,NULL,0,'b90e8c11-9258-44ee-901a-1650b6e14601','openid-connect',0,0,0,'${client_account}',0,'client-secret','${authBaseUrl}',NULL,NULL,1,0,0,0),('703aca18-8a9e-44ea-bf88-648ae965c214',1,0,'account-console',0,1,NULL,'/realms/master/account/',0,NULL,0,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','openid-connect',0,0,0,'${client_account-console}',0,'client-secret','${authBaseUrl}',NULL,NULL,1,0,0,0),('70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,0,'account',0,1,NULL,'/realms/master/account/',0,NULL,0,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','openid-connect',0,0,0,'${client_account}',0,'client-secret','${authBaseUrl}',NULL,NULL,1,0,0,0),('7793a774-b465-4b0b-97d1-08840aa08c9a',1,1,'security-admin-console',0,1,NULL,'/admin/keycloak/console/',0,NULL,0,'b90e8c11-9258-44ee-901a-1650b6e14601','openid-connect',0,0,0,'${client_security-admin-console}',0,'client-secret','${authAdminUrl}',NULL,NULL,1,0,0,0),('8c7f02a6-c93c-483c-b13e-dd352ad4d023',1,1,'admin-cli',0,1,NULL,NULL,0,NULL,0,'b90e8c11-9258-44ee-901a-1650b6e14601','openid-connect',0,0,0,'${client_admin-cli}',0,'client-secret',NULL,NULL,NULL,0,0,1,0),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5',1,0,'account-console',0,1,NULL,'/realms/keycloak/account/',0,NULL,0,'b90e8c11-9258-44ee-901a-1650b6e14601','openid-connect',0,0,0,'${client_account-console}',0,'client-secret','${authBaseUrl}',NULL,NULL,1,0,0,0),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767',1,1,'admin-cli',0,1,NULL,NULL,0,NULL,0,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','openid-connect',0,0,0,'${client_admin-cli}',0,'client-secret',NULL,NULL,NULL,0,0,1,0),('ba733c9c-d2a2-4464-a77d-d6b393a36968',1,0,'broker',0,0,NULL,NULL,1,NULL,0,'b90e8c11-9258-44ee-901a-1650b6e14601','openid-connect',0,0,0,'${client_broker}',0,'client-secret',NULL,NULL,NULL,1,0,0,0),('e12fceb4-c28d-4e75-8f26-48fe7824c110',1,1,'security-admin-console',0,1,NULL,'/admin/master/console/',0,NULL,0,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','openid-connect',0,0,0,'${client_security-admin-console}',0,'client-secret','${authAdminUrl}',NULL,NULL,1,0,0,0);
/*!40000 ALTER TABLE `CLIENT` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_ATTRIBUTES`
--

DROP TABLE IF EXISTS `CLIENT_ATTRIBUTES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_ATTRIBUTES` (
  `CLIENT_ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  PRIMARY KEY (`CLIENT_ID`,`NAME`),
  KEY `IDX_CLIENT_ATT_BY_NAME_VALUE` (`NAME`,`VALUE`(255)),
  CONSTRAINT `FK3C47C64BEACCA966` FOREIGN KEY (`CLIENT_ID`) REFERENCES `CLIENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_ATTRIBUTES`
--

/*!40000 ALTER TABLE `CLIENT_ATTRIBUTES` DISABLE KEYS */;
INSERT INTO `CLIENT_ATTRIBUTES` VALUES ('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','access.token.header.type.rfc9068','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','acr.loa.map','{}'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','backchannel.logout.revoke.offline.tokens','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','backchannel.logout.session.required','true'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','client_credentials.use_refresh_token','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','client.introspection.response.allow.jwt.claim.enabled','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','client.secret.creation.time','1751813603'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','client.use.lightweight.access.token.enabled','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','display.on.consent.screen','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','frontchannel.logout.session.required','true'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','oauth2.device.authorization.grant.enabled','true'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','oidc.ciba.grant.enabled','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','post.logout.redirect.uris','*'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','realm_client','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','request.object.encryption.alg','any'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','request.object.encryption.enc','any'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','request.object.required','not required'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','request.object.signature.alg','any'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','require.pushed.authorization.requests','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','standard.token.exchange.enabled','true'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','tls.client.certificate.bound.access.tokens','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','token.response.type.bearer.lower-case','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','use.jwks.url','false'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','use.refresh.tokens','true'),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','post.logout.redirect.uris','+'),('703aca18-8a9e-44ea-bf88-648ae965c214','pkce.code.challenge.method','S256'),('703aca18-8a9e-44ea-bf88-648ae965c214','post.logout.redirect.uris','+'),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','post.logout.redirect.uris','+'),('7793a774-b465-4b0b-97d1-08840aa08c9a','client.use.lightweight.access.token.enabled','true'),('7793a774-b465-4b0b-97d1-08840aa08c9a','pkce.code.challenge.method','S256'),('7793a774-b465-4b0b-97d1-08840aa08c9a','post.logout.redirect.uris','+'),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','client.use.lightweight.access.token.enabled','true'),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','pkce.code.challenge.method','S256'),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','post.logout.redirect.uris','+'),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','client.use.lightweight.access.token.enabled','true'),('e12fceb4-c28d-4e75-8f26-48fe7824c110','client.use.lightweight.access.token.enabled','true'),('e12fceb4-c28d-4e75-8f26-48fe7824c110','pkce.code.challenge.method','S256'),('e12fceb4-c28d-4e75-8f26-48fe7824c110','post.logout.redirect.uris','+');
/*!40000 ALTER TABLE `CLIENT_ATTRIBUTES` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_AUTH_FLOW_BINDINGS`
--

DROP TABLE IF EXISTS `CLIENT_AUTH_FLOW_BINDINGS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_AUTH_FLOW_BINDINGS` (
  `CLIENT_ID` varchar(36) NOT NULL,
  `FLOW_ID` varchar(36) DEFAULT NULL,
  `BINDING_NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`CLIENT_ID`,`BINDING_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_AUTH_FLOW_BINDINGS`
--

/*!40000 ALTER TABLE `CLIENT_AUTH_FLOW_BINDINGS` DISABLE KEYS */;
INSERT INTO `CLIENT_AUTH_FLOW_BINDINGS` VALUES ('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','919e97ab-8fa3-4795-b38b-cef08e92bd01','browser'),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','c75b3bc1-3d7e-4e5c-85bb-0988ffae04e8','direct_grant');
/*!40000 ALTER TABLE `CLIENT_AUTH_FLOW_BINDINGS` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_INITIAL_ACCESS`
--

DROP TABLE IF EXISTS `CLIENT_INITIAL_ACCESS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_INITIAL_ACCESS` (
  `ID` varchar(36) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `TIMESTAMP` int DEFAULT NULL,
  `EXPIRATION` int DEFAULT NULL,
  `COUNT` int DEFAULT NULL,
  `REMAINING_COUNT` int DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_CLIENT_INIT_ACC_REALM` (`REALM_ID`),
  CONSTRAINT `FK_CLIENT_INIT_ACC_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_INITIAL_ACCESS`
--

/*!40000 ALTER TABLE `CLIENT_INITIAL_ACCESS` DISABLE KEYS */;
/*!40000 ALTER TABLE `CLIENT_INITIAL_ACCESS` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_NODE_REGISTRATIONS`
--

DROP TABLE IF EXISTS `CLIENT_NODE_REGISTRATIONS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_NODE_REGISTRATIONS` (
  `CLIENT_ID` varchar(36) NOT NULL,
  `VALUE` int DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`CLIENT_ID`,`NAME`),
  CONSTRAINT `FK4129723BA992F594` FOREIGN KEY (`CLIENT_ID`) REFERENCES `CLIENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_NODE_REGISTRATIONS`
--

/*!40000 ALTER TABLE `CLIENT_NODE_REGISTRATIONS` DISABLE KEYS */;
/*!40000 ALTER TABLE `CLIENT_NODE_REGISTRATIONS` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_SCOPE`
--

DROP TABLE IF EXISTS `CLIENT_SCOPE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_SCOPE` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `PROTOCOL` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_CLI_SCOPE` (`REALM_ID`,`NAME`),
  KEY `IDX_REALM_CLSCOPE` (`REALM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_SCOPE`
--

/*!40000 ALTER TABLE `CLIENT_SCOPE` DISABLE KEYS */;
INSERT INTO `CLIENT_SCOPE` VALUES ('0a69cb43-9249-465f-8bea-e8097fd91e24','profile','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect built-in scope: profile','openid-connect'),('1ae58ee6-26ff-4b6f-84e5-f57d18b92b17','offline_access','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect built-in scope: offline_access','openid-connect'),('324d157a-d679-4372-adbd-a8f13aa424af','organization','b90e8c11-9258-44ee-901a-1650b6e14601','Additional claims about the organization a subject belongs to','openid-connect'),('33dba1c8-8c1c-4128-b02f-4f10b90f97a2','web-origins','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect scope for add allowed web origins to the access token','openid-connect'),('3ae50474-5f28-4562-baa3-4ee4463c183f','saml_organization','736d3e5b-4c46-41ed-9afb-47e727ecab9e','Organization Membership','saml'),('44bc3d1e-d675-4a76-b1a8-c976fbd843cd','roles','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect scope for add user roles to the access token','openid-connect'),('453424f4-1a6e-412f-ace0-7c92331cc1e4','phone','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect built-in scope: phone','openid-connect'),('47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f','microprofile-jwt','736d3e5b-4c46-41ed-9afb-47e727ecab9e','Microprofile - JWT built-in scope','openid-connect'),('5139b7db-f531-479a-91b1-af0146f86a7c','role_list','b90e8c11-9258-44ee-901a-1650b6e14601','SAML role list','saml'),('517304c6-4e73-4a1a-ae1c-89de779c6d86','address','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect built-in scope: address','openid-connect'),('5619f20a-e951-410e-af88-38084f0f498b','phone','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect built-in scope: phone','openid-connect'),('6c1afe15-15bd-436c-b923-abfdc3701b4e','email','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect built-in scope: email','openid-connect'),('9d152c27-e725-4a7e-9984-d18e62b07911','address','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect built-in scope: address','openid-connect'),('a1327838-8323-46e5-b4c2-85da961df597','basic','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect scope for add all basic claims to the token','openid-connect'),('a4a4fdcf-e5be-4d5e-8874-2e6ddfc7cd34','service_account','b90e8c11-9258-44ee-901a-1650b6e14601','Specific scope for a client enabled for service accounts','openid-connect'),('aaa4253c-d77a-4b6e-a67a-2dab2ee49116','acr','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect scope for add acr (authentication context class reference) to the token','openid-connect'),('b503aa77-481f-4457-ad9b-1c444f7d82bd','web-origins','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect scope for add allowed web origins to the access token','openid-connect'),('c22b7fda-6a7d-482d-ac0c-8a8301050d83','offline_access','b90e8c11-9258-44ee-901a-1650b6e14601','OpenID Connect built-in scope: offline_access','openid-connect'),('c2f2e082-e87c-419d-af80-8803991286f6','roles','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect scope for add user roles to the access token','openid-connect'),('c5ba5bb8-8431-4542-acd4-f9de2730c7d1','acr','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect scope for add acr (authentication context class reference) to the token','openid-connect'),('ceb884c9-b61b-4b39-af8f-e5c63106791b','role_list','736d3e5b-4c46-41ed-9afb-47e727ecab9e','SAML role list','saml'),('d59644b7-45b9-4c40-9004-ad0f4c922689','email','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect built-in scope: email','openid-connect'),('e61927e1-ec5d-4137-8617-31b436b5c9be','basic','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect scope for add all basic claims to the token','openid-connect'),('e94865e0-603b-4582-b7d4-90151939f139','service_account','736d3e5b-4c46-41ed-9afb-47e727ecab9e','Specific scope for a client enabled for service accounts','openid-connect'),('eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7','organization','736d3e5b-4c46-41ed-9afb-47e727ecab9e','Additional claims about the organization a subject belongs to','openid-connect'),('f13e4778-609b-484d-affb-3ba1856b32e0','microprofile-jwt','b90e8c11-9258-44ee-901a-1650b6e14601','Microprofile - JWT built-in scope','openid-connect'),('f420fc2a-382f-4dd0-8d2e-ac376bd22c32','saml_organization','b90e8c11-9258-44ee-901a-1650b6e14601','Organization Membership','saml'),('fdd5034a-7fc4-499e-9eaa-3544210e2dd1','profile','736d3e5b-4c46-41ed-9afb-47e727ecab9e','OpenID Connect built-in scope: profile','openid-connect');
/*!40000 ALTER TABLE `CLIENT_SCOPE` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_SCOPE_ATTRIBUTES`
--

DROP TABLE IF EXISTS `CLIENT_SCOPE_ATTRIBUTES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_SCOPE_ATTRIBUTES` (
  `SCOPE_ID` varchar(36) NOT NULL,
  `VALUE` text,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`SCOPE_ID`,`NAME`),
  KEY `IDX_CLSCOPE_ATTRS` (`SCOPE_ID`),
  CONSTRAINT `FK_CL_SCOPE_ATTR_SCOPE` FOREIGN KEY (`SCOPE_ID`) REFERENCES `CLIENT_SCOPE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_SCOPE_ATTRIBUTES`
--

/*!40000 ALTER TABLE `CLIENT_SCOPE_ATTRIBUTES` DISABLE KEYS */;
INSERT INTO `CLIENT_SCOPE_ATTRIBUTES` VALUES ('0a69cb43-9249-465f-8bea-e8097fd91e24','${profileScopeConsentText}','consent.screen.text'),('0a69cb43-9249-465f-8bea-e8097fd91e24','true','display.on.consent.screen'),('0a69cb43-9249-465f-8bea-e8097fd91e24','true','include.in.token.scope'),('1ae58ee6-26ff-4b6f-84e5-f57d18b92b17','${offlineAccessScopeConsentText}','consent.screen.text'),('1ae58ee6-26ff-4b6f-84e5-f57d18b92b17','true','display.on.consent.screen'),('324d157a-d679-4372-adbd-a8f13aa424af','${organizationScopeConsentText}','consent.screen.text'),('324d157a-d679-4372-adbd-a8f13aa424af','true','display.on.consent.screen'),('324d157a-d679-4372-adbd-a8f13aa424af','true','include.in.token.scope'),('33dba1c8-8c1c-4128-b02f-4f10b90f97a2','','consent.screen.text'),('33dba1c8-8c1c-4128-b02f-4f10b90f97a2','false','display.on.consent.screen'),('33dba1c8-8c1c-4128-b02f-4f10b90f97a2','false','include.in.token.scope'),('3ae50474-5f28-4562-baa3-4ee4463c183f','false','display.on.consent.screen'),('44bc3d1e-d675-4a76-b1a8-c976fbd843cd','${rolesScopeConsentText}','consent.screen.text'),('44bc3d1e-d675-4a76-b1a8-c976fbd843cd','true','display.on.consent.screen'),('44bc3d1e-d675-4a76-b1a8-c976fbd843cd','false','include.in.token.scope'),('453424f4-1a6e-412f-ace0-7c92331cc1e4','${phoneScopeConsentText}','consent.screen.text'),('453424f4-1a6e-412f-ace0-7c92331cc1e4','true','display.on.consent.screen'),('453424f4-1a6e-412f-ace0-7c92331cc1e4','true','include.in.token.scope'),('47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f','false','display.on.consent.screen'),('47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f','true','include.in.token.scope'),('5139b7db-f531-479a-91b1-af0146f86a7c','${samlRoleListScopeConsentText}','consent.screen.text'),('5139b7db-f531-479a-91b1-af0146f86a7c','true','display.on.consent.screen'),('517304c6-4e73-4a1a-ae1c-89de779c6d86','${addressScopeConsentText}','consent.screen.text'),('517304c6-4e73-4a1a-ae1c-89de779c6d86','true','display.on.consent.screen'),('517304c6-4e73-4a1a-ae1c-89de779c6d86','true','include.in.token.scope'),('5619f20a-e951-410e-af88-38084f0f498b','${phoneScopeConsentText}','consent.screen.text'),('5619f20a-e951-410e-af88-38084f0f498b','true','display.on.consent.screen'),('5619f20a-e951-410e-af88-38084f0f498b','true','include.in.token.scope'),('6c1afe15-15bd-436c-b923-abfdc3701b4e','${emailScopeConsentText}','consent.screen.text'),('6c1afe15-15bd-436c-b923-abfdc3701b4e','true','display.on.consent.screen'),('6c1afe15-15bd-436c-b923-abfdc3701b4e','true','include.in.token.scope'),('9d152c27-e725-4a7e-9984-d18e62b07911','${addressScopeConsentText}','consent.screen.text'),('9d152c27-e725-4a7e-9984-d18e62b07911','true','display.on.consent.screen'),('9d152c27-e725-4a7e-9984-d18e62b07911','true','include.in.token.scope'),('a1327838-8323-46e5-b4c2-85da961df597','false','display.on.consent.screen'),('a1327838-8323-46e5-b4c2-85da961df597','false','include.in.token.scope'),('a4a4fdcf-e5be-4d5e-8874-2e6ddfc7cd34','false','display.on.consent.screen'),('a4a4fdcf-e5be-4d5e-8874-2e6ddfc7cd34','false','include.in.token.scope'),('aaa4253c-d77a-4b6e-a67a-2dab2ee49116','false','display.on.consent.screen'),('aaa4253c-d77a-4b6e-a67a-2dab2ee49116','false','include.in.token.scope'),('b503aa77-481f-4457-ad9b-1c444f7d82bd','','consent.screen.text'),('b503aa77-481f-4457-ad9b-1c444f7d82bd','false','display.on.consent.screen'),('b503aa77-481f-4457-ad9b-1c444f7d82bd','false','include.in.token.scope'),('c22b7fda-6a7d-482d-ac0c-8a8301050d83','${offlineAccessScopeConsentText}','consent.screen.text'),('c22b7fda-6a7d-482d-ac0c-8a8301050d83','true','display.on.consent.screen'),('c2f2e082-e87c-419d-af80-8803991286f6','${rolesScopeConsentText}','consent.screen.text'),('c2f2e082-e87c-419d-af80-8803991286f6','true','display.on.consent.screen'),('c2f2e082-e87c-419d-af80-8803991286f6','false','include.in.token.scope'),('c5ba5bb8-8431-4542-acd4-f9de2730c7d1','false','display.on.consent.screen'),('c5ba5bb8-8431-4542-acd4-f9de2730c7d1','false','include.in.token.scope'),('ceb884c9-b61b-4b39-af8f-e5c63106791b','${samlRoleListScopeConsentText}','consent.screen.text'),('ceb884c9-b61b-4b39-af8f-e5c63106791b','true','display.on.consent.screen'),('d59644b7-45b9-4c40-9004-ad0f4c922689','${emailScopeConsentText}','consent.screen.text'),('d59644b7-45b9-4c40-9004-ad0f4c922689','true','display.on.consent.screen'),('d59644b7-45b9-4c40-9004-ad0f4c922689','true','include.in.token.scope'),('e61927e1-ec5d-4137-8617-31b436b5c9be','false','display.on.consent.screen'),('e61927e1-ec5d-4137-8617-31b436b5c9be','false','include.in.token.scope'),('e94865e0-603b-4582-b7d4-90151939f139','false','display.on.consent.screen'),('e94865e0-603b-4582-b7d4-90151939f139','false','include.in.token.scope'),('eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7','${organizationScopeConsentText}','consent.screen.text'),('eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7','true','display.on.consent.screen'),('eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7','true','include.in.token.scope'),('f13e4778-609b-484d-affb-3ba1856b32e0','false','display.on.consent.screen'),('f13e4778-609b-484d-affb-3ba1856b32e0','true','include.in.token.scope'),('f420fc2a-382f-4dd0-8d2e-ac376bd22c32','false','display.on.consent.screen'),('fdd5034a-7fc4-499e-9eaa-3544210e2dd1','${profileScopeConsentText}','consent.screen.text'),('fdd5034a-7fc4-499e-9eaa-3544210e2dd1','true','display.on.consent.screen'),('fdd5034a-7fc4-499e-9eaa-3544210e2dd1','true','include.in.token.scope');
/*!40000 ALTER TABLE `CLIENT_SCOPE_ATTRIBUTES` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_SCOPE_CLIENT`
--

DROP TABLE IF EXISTS `CLIENT_SCOPE_CLIENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_SCOPE_CLIENT` (
  `CLIENT_ID` varchar(255) NOT NULL,
  `SCOPE_ID` varchar(255) NOT NULL,
  `DEFAULT_SCOPE` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`CLIENT_ID`,`SCOPE_ID`),
  KEY `IDX_CLSCOPE_CL` (`CLIENT_ID`),
  KEY `IDX_CL_CLSCOPE` (`SCOPE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_SCOPE_CLIENT`
--

/*!40000 ALTER TABLE `CLIENT_SCOPE_CLIENT` DISABLE KEYS */;
INSERT INTO `CLIENT_SCOPE_CLIENT` VALUES ('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','324d157a-d679-4372-adbd-a8f13aa424af',0),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','a1327838-8323-46e5-b4c2-85da961df597',1),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','a4a4fdcf-e5be-4d5e-8874-2e6ddfc7cd34',1),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','f13e4778-609b-484d-affb-3ba1856b32e0',0),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','324d157a-d679-4372-adbd-a8f13aa424af',0),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','a1327838-8323-46e5-b4c2-85da961df597',1),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('1aa38202-97c8-4c48-9f97-d1b65bdb84c8','f13e4778-609b-484d-affb-3ba1856b32e0',0),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','1ae58ee6-26ff-4b6f-84e5-f57d18b92b17',0),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f',0),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','5619f20a-e951-410e-af88-38084f0f498b',0),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','9d152c27-e725-4a7e-9984-d18e62b07911',0),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','b503aa77-481f-4457-ad9b-1c444f7d82bd',1),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','c2f2e082-e87c-419d-af80-8803991286f6',1),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','c5ba5bb8-8431-4542-acd4-f9de2730c7d1',1),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','d59644b7-45b9-4c40-9004-ad0f4c922689',1),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','e61927e1-ec5d-4137-8617-31b436b5c9be',1),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7',0),('3fc95085-aba5-4a9e-95d5-532e3afad9f2','fdd5034a-7fc4-499e-9eaa-3544210e2dd1',1),('540a1782-e266-4d93-8898-fd85e3ca4825','1ae58ee6-26ff-4b6f-84e5-f57d18b92b17',0),('540a1782-e266-4d93-8898-fd85e3ca4825','47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f',0),('540a1782-e266-4d93-8898-fd85e3ca4825','5619f20a-e951-410e-af88-38084f0f498b',0),('540a1782-e266-4d93-8898-fd85e3ca4825','9d152c27-e725-4a7e-9984-d18e62b07911',0),('540a1782-e266-4d93-8898-fd85e3ca4825','b503aa77-481f-4457-ad9b-1c444f7d82bd',1),('540a1782-e266-4d93-8898-fd85e3ca4825','c2f2e082-e87c-419d-af80-8803991286f6',1),('540a1782-e266-4d93-8898-fd85e3ca4825','c5ba5bb8-8431-4542-acd4-f9de2730c7d1',1),('540a1782-e266-4d93-8898-fd85e3ca4825','d59644b7-45b9-4c40-9004-ad0f4c922689',1),('540a1782-e266-4d93-8898-fd85e3ca4825','e61927e1-ec5d-4137-8617-31b436b5c9be',1),('540a1782-e266-4d93-8898-fd85e3ca4825','eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7',0),('540a1782-e266-4d93-8898-fd85e3ca4825','fdd5034a-7fc4-499e-9eaa-3544210e2dd1',1),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','324d157a-d679-4372-adbd-a8f13aa424af',0),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','a1327838-8323-46e5-b4c2-85da961df597',1),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','f13e4778-609b-484d-affb-3ba1856b32e0',0),('703aca18-8a9e-44ea-bf88-648ae965c214','1ae58ee6-26ff-4b6f-84e5-f57d18b92b17',0),('703aca18-8a9e-44ea-bf88-648ae965c214','47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f',0),('703aca18-8a9e-44ea-bf88-648ae965c214','5619f20a-e951-410e-af88-38084f0f498b',0),('703aca18-8a9e-44ea-bf88-648ae965c214','9d152c27-e725-4a7e-9984-d18e62b07911',0),('703aca18-8a9e-44ea-bf88-648ae965c214','b503aa77-481f-4457-ad9b-1c444f7d82bd',1),('703aca18-8a9e-44ea-bf88-648ae965c214','c2f2e082-e87c-419d-af80-8803991286f6',1),('703aca18-8a9e-44ea-bf88-648ae965c214','c5ba5bb8-8431-4542-acd4-f9de2730c7d1',1),('703aca18-8a9e-44ea-bf88-648ae965c214','d59644b7-45b9-4c40-9004-ad0f4c922689',1),('703aca18-8a9e-44ea-bf88-648ae965c214','e61927e1-ec5d-4137-8617-31b436b5c9be',1),('703aca18-8a9e-44ea-bf88-648ae965c214','eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7',0),('703aca18-8a9e-44ea-bf88-648ae965c214','fdd5034a-7fc4-499e-9eaa-3544210e2dd1',1),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','1ae58ee6-26ff-4b6f-84e5-f57d18b92b17',0),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f',0),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','5619f20a-e951-410e-af88-38084f0f498b',0),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','9d152c27-e725-4a7e-9984-d18e62b07911',0),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','b503aa77-481f-4457-ad9b-1c444f7d82bd',1),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','c2f2e082-e87c-419d-af80-8803991286f6',1),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','c5ba5bb8-8431-4542-acd4-f9de2730c7d1',1),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','d59644b7-45b9-4c40-9004-ad0f4c922689',1),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','e61927e1-ec5d-4137-8617-31b436b5c9be',1),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7',0),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','fdd5034a-7fc4-499e-9eaa-3544210e2dd1',1),('7793a774-b465-4b0b-97d1-08840aa08c9a','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('7793a774-b465-4b0b-97d1-08840aa08c9a','324d157a-d679-4372-adbd-a8f13aa424af',0),('7793a774-b465-4b0b-97d1-08840aa08c9a','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('7793a774-b465-4b0b-97d1-08840aa08c9a','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('7793a774-b465-4b0b-97d1-08840aa08c9a','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('7793a774-b465-4b0b-97d1-08840aa08c9a','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('7793a774-b465-4b0b-97d1-08840aa08c9a','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('7793a774-b465-4b0b-97d1-08840aa08c9a','a1327838-8323-46e5-b4c2-85da961df597',1),('7793a774-b465-4b0b-97d1-08840aa08c9a','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('7793a774-b465-4b0b-97d1-08840aa08c9a','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('7793a774-b465-4b0b-97d1-08840aa08c9a','f13e4778-609b-484d-affb-3ba1856b32e0',0),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','324d157a-d679-4372-adbd-a8f13aa424af',0),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','a1327838-8323-46e5-b4c2-85da961df597',1),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('8c7f02a6-c93c-483c-b13e-dd352ad4d023','f13e4778-609b-484d-affb-3ba1856b32e0',0),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','324d157a-d679-4372-adbd-a8f13aa424af',0),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','a1327838-8323-46e5-b4c2-85da961df597',1),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','f13e4778-609b-484d-affb-3ba1856b32e0',0),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','1ae58ee6-26ff-4b6f-84e5-f57d18b92b17',0),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f',0),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','5619f20a-e951-410e-af88-38084f0f498b',0),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','9d152c27-e725-4a7e-9984-d18e62b07911',0),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','b503aa77-481f-4457-ad9b-1c444f7d82bd',1),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','c2f2e082-e87c-419d-af80-8803991286f6',1),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','c5ba5bb8-8431-4542-acd4-f9de2730c7d1',1),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','d59644b7-45b9-4c40-9004-ad0f4c922689',1),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','e61927e1-ec5d-4137-8617-31b436b5c9be',1),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7',0),('b6f2fd45-e4bb-40d0-aa02-4efe056e6767','fdd5034a-7fc4-499e-9eaa-3544210e2dd1',1),('ba733c9c-d2a2-4464-a77d-d6b393a36968','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('ba733c9c-d2a2-4464-a77d-d6b393a36968','324d157a-d679-4372-adbd-a8f13aa424af',0),('ba733c9c-d2a2-4464-a77d-d6b393a36968','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('ba733c9c-d2a2-4464-a77d-d6b393a36968','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('ba733c9c-d2a2-4464-a77d-d6b393a36968','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('ba733c9c-d2a2-4464-a77d-d6b393a36968','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('ba733c9c-d2a2-4464-a77d-d6b393a36968','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('ba733c9c-d2a2-4464-a77d-d6b393a36968','a1327838-8323-46e5-b4c2-85da961df597',1),('ba733c9c-d2a2-4464-a77d-d6b393a36968','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('ba733c9c-d2a2-4464-a77d-d6b393a36968','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('ba733c9c-d2a2-4464-a77d-d6b393a36968','f13e4778-609b-484d-affb-3ba1856b32e0',0),('e12fceb4-c28d-4e75-8f26-48fe7824c110','1ae58ee6-26ff-4b6f-84e5-f57d18b92b17',0),('e12fceb4-c28d-4e75-8f26-48fe7824c110','47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f',0),('e12fceb4-c28d-4e75-8f26-48fe7824c110','5619f20a-e951-410e-af88-38084f0f498b',0),('e12fceb4-c28d-4e75-8f26-48fe7824c110','9d152c27-e725-4a7e-9984-d18e62b07911',0),('e12fceb4-c28d-4e75-8f26-48fe7824c110','b503aa77-481f-4457-ad9b-1c444f7d82bd',1),('e12fceb4-c28d-4e75-8f26-48fe7824c110','c2f2e082-e87c-419d-af80-8803991286f6',1),('e12fceb4-c28d-4e75-8f26-48fe7824c110','c5ba5bb8-8431-4542-acd4-f9de2730c7d1',1),('e12fceb4-c28d-4e75-8f26-48fe7824c110','d59644b7-45b9-4c40-9004-ad0f4c922689',1),('e12fceb4-c28d-4e75-8f26-48fe7824c110','e61927e1-ec5d-4137-8617-31b436b5c9be',1),('e12fceb4-c28d-4e75-8f26-48fe7824c110','eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7',0),('e12fceb4-c28d-4e75-8f26-48fe7824c110','fdd5034a-7fc4-499e-9eaa-3544210e2dd1',1);
/*!40000 ALTER TABLE `CLIENT_SCOPE_CLIENT` ENABLE KEYS */;

--
-- Table structure for table `CLIENT_SCOPE_ROLE_MAPPING`
--

DROP TABLE IF EXISTS `CLIENT_SCOPE_ROLE_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CLIENT_SCOPE_ROLE_MAPPING` (
  `SCOPE_ID` varchar(36) NOT NULL,
  `ROLE_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`SCOPE_ID`,`ROLE_ID`),
  KEY `IDX_CLSCOPE_ROLE` (`SCOPE_ID`),
  KEY `IDX_ROLE_CLSCOPE` (`ROLE_ID`),
  CONSTRAINT `FK_CL_SCOPE_RM_SCOPE` FOREIGN KEY (`SCOPE_ID`) REFERENCES `CLIENT_SCOPE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CLIENT_SCOPE_ROLE_MAPPING`
--

/*!40000 ALTER TABLE `CLIENT_SCOPE_ROLE_MAPPING` DISABLE KEYS */;
INSERT INTO `CLIENT_SCOPE_ROLE_MAPPING` VALUES ('1ae58ee6-26ff-4b6f-84e5-f57d18b92b17','06bd840d-d8b2-4262-92e9-efc268fc5bd4'),('c22b7fda-6a7d-482d-ac0c-8a8301050d83','bac7bce2-1afa-4164-a4e1-54d36c788834');
/*!40000 ALTER TABLE `CLIENT_SCOPE_ROLE_MAPPING` ENABLE KEYS */;

--
-- Table structure for table `COMPONENT`
--

DROP TABLE IF EXISTS `COMPONENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `COMPONENT` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `PARENT_ID` varchar(36) DEFAULT NULL,
  `PROVIDER_ID` varchar(36) DEFAULT NULL,
  `PROVIDER_TYPE` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  `SUB_TYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_COMPONENT_REALM` (`REALM_ID`),
  KEY `IDX_COMPONENT_PROVIDER_TYPE` (`PROVIDER_TYPE`),
  CONSTRAINT `FK_COMPONENT_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `COMPONENT`
--

/*!40000 ALTER TABLE `COMPONENT` DISABLE KEYS */;
INSERT INTO `COMPONENT` VALUES ('0ebc0f38-1c42-4e68-bd83-07d651604117','Allowed Protocol Mapper Types','736d3e5b-4c46-41ed-9afb-47e727ecab9e','allowed-protocol-mappers','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','authenticated'),('1defc255-2f5f-4e7f-a51c-e75d28687d06','Allowed Client Scopes','736d3e5b-4c46-41ed-9afb-47e727ecab9e','allowed-client-templates','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','anonymous'),('22cf7c67-4e40-490e-890a-1e257f4d8eb3','rsa-generated','736d3e5b-4c46-41ed-9afb-47e727ecab9e','rsa-generated','org.keycloak.keys.KeyProvider','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL),('2ab2cd57-9a34-4a7b-93b7-e91997df03d3','aes-generated','736d3e5b-4c46-41ed-9afb-47e727ecab9e','aes-generated','org.keycloak.keys.KeyProvider','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL),('361ebca1-f1d3-48cc-9836-b82bb962d15c','Trusted Hosts','736d3e5b-4c46-41ed-9afb-47e727ecab9e','trusted-hosts','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','anonymous'),('392c75f9-c9db-4c97-be1a-f9f09e711e3c','aes-generated','b90e8c11-9258-44ee-901a-1650b6e14601','aes-generated','org.keycloak.keys.KeyProvider','b90e8c11-9258-44ee-901a-1650b6e14601',NULL),('3fceb816-0a49-46eb-b4e2-3b410f0d08cd','hmac-generated-hs512','b90e8c11-9258-44ee-901a-1650b6e14601','hmac-generated','org.keycloak.keys.KeyProvider','b90e8c11-9258-44ee-901a-1650b6e14601',NULL),('42059290-819f-4c0a-8ad7-d05a5d9688ba','rsa-enc-generated','736d3e5b-4c46-41ed-9afb-47e727ecab9e','rsa-enc-generated','org.keycloak.keys.KeyProvider','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL),('4313873c-9704-4758-bc1a-61a9a47c6454','Consent Required','b90e8c11-9258-44ee-901a-1650b6e14601','consent-required','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','anonymous'),('43457e00-86c5-4cf0-957a-fd3d31bffed5','Full Scope Disabled','b90e8c11-9258-44ee-901a-1650b6e14601','scope','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','anonymous'),('50f6a4e5-e3d2-44b3-82f6-437e29721804','rsa-generated','b90e8c11-9258-44ee-901a-1650b6e14601','rsa-generated','org.keycloak.keys.KeyProvider','b90e8c11-9258-44ee-901a-1650b6e14601',NULL),('5a0d223c-5606-4bf9-a85b-bfdd1cfb6324','Consent Required','736d3e5b-4c46-41ed-9afb-47e727ecab9e','consent-required','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','anonymous'),('688b0a51-9926-4e9c-8490-a318b5983837','Allowed Protocol Mapper Types','b90e8c11-9258-44ee-901a-1650b6e14601','allowed-protocol-mappers','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','authenticated'),('6acbd477-28bb-46f6-b974-272b8b5db1f5','Allowed Client Scopes','736d3e5b-4c46-41ed-9afb-47e727ecab9e','allowed-client-templates','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','authenticated'),('70b49c9e-843d-4515-a308-12a9f605e510','Allowed Protocol Mapper Types','736d3e5b-4c46-41ed-9afb-47e727ecab9e','allowed-protocol-mappers','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','anonymous'),('7fc12977-b66a-4914-b20a-03bc53aa6338','Max Clients Limit','b90e8c11-9258-44ee-901a-1650b6e14601','max-clients','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','anonymous'),('88604e8a-c8d7-40af-ba77-17969f2190ff','rsa-enc-generated','b90e8c11-9258-44ee-901a-1650b6e14601','rsa-enc-generated','org.keycloak.keys.KeyProvider','b90e8c11-9258-44ee-901a-1650b6e14601',NULL),('92fde300-ed76-454f-8fd9-4659ca2329a1','Allowed Client Scopes','b90e8c11-9258-44ee-901a-1650b6e14601','allowed-client-templates','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','authenticated'),('a20d3c8c-96f7-414c-aa97-65dcbf516d0d','Full Scope Disabled','736d3e5b-4c46-41ed-9afb-47e727ecab9e','scope','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','anonymous'),('a5c75855-4661-4655-b4b0-c0fb8514742a','hmac-generated-hs512','736d3e5b-4c46-41ed-9afb-47e727ecab9e','hmac-generated','org.keycloak.keys.KeyProvider','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL),('bb63ad2b-b90a-4405-9f2e-e6ce04f9cfa1','Trusted Hosts','b90e8c11-9258-44ee-901a-1650b6e14601','trusted-hosts','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','anonymous'),('cf8950f9-fdf3-41e5-abd6-18af017ad108','Max Clients Limit','736d3e5b-4c46-41ed-9afb-47e727ecab9e','max-clients','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','anonymous'),('d6c0e9c5-c5da-4f84-b3a8-7568fddc9922','Allowed Client Scopes','b90e8c11-9258-44ee-901a-1650b6e14601','allowed-client-templates','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','anonymous'),('d94199c3-206d-4b00-9c87-5d956182b536',NULL,'736d3e5b-4c46-41ed-9afb-47e727ecab9e','declarative-user-profile','org.keycloak.userprofile.UserProfileProvider','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL),('f51cae22-a61d-4702-979b-e9a468b1c67d','Allowed Protocol Mapper Types','b90e8c11-9258-44ee-901a-1650b6e14601','allowed-protocol-mappers','org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','anonymous');
/*!40000 ALTER TABLE `COMPONENT` ENABLE KEYS */;

--
-- Table structure for table `COMPONENT_CONFIG`
--

DROP TABLE IF EXISTS `COMPONENT_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `COMPONENT_CONFIG` (
  `ID` varchar(36) NOT NULL,
  `COMPONENT_ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  PRIMARY KEY (`ID`),
  KEY `IDX_COMPO_CONFIG_COMPO` (`COMPONENT_ID`),
  CONSTRAINT `FK_COMPONENT_CONFIG` FOREIGN KEY (`COMPONENT_ID`) REFERENCES `COMPONENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `COMPONENT_CONFIG`
--

/*!40000 ALTER TABLE `COMPONENT_CONFIG` DISABLE KEYS */;
INSERT INTO `COMPONENT_CONFIG` VALUES ('043f5268-2ef1-437e-91ca-3488792009c1','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','oidc-full-name-mapper'),('0843014e-faff-4ffd-bd1d-98d4a7f2ce88','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','oidc-sha256-pairwise-sub-mapper'),('093ef402-feb1-4623-95b2-43fa93ddad88','50f6a4e5-e3d2-44b3-82f6-437e29721804','keyUse','SIG'),('0b9efdda-d216-4fb5-bc3d-43773fe9fd07','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','saml-role-list-mapper'),('11c2b61c-4875-46ef-8f0e-62ac2edd390b','42059290-819f-4c0a-8ad7-d05a5d9688ba','algorithm','RSA-OAEP'),('13a108e0-a6f8-428a-935f-4a2dc6255094','392c75f9-c9db-4c97-be1a-f9f09e711e3c','kid','36785980-af4f-4874-8f68-7415ca907d72'),('19b58fc9-ee54-432d-9ed4-c6e5a003b344','392c75f9-c9db-4c97-be1a-f9f09e711e3c','secret','MRXIzVlgBvhC6sfVKfOZNg'),('1a2e9ae1-1d17-41a3-9123-300500d55577','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','saml-user-property-mapper'),('1a6b3644-5cf2-4794-a07f-ffeadd300931','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','oidc-full-name-mapper'),('1be012bd-fad1-4dac-b156-352352033b56','d6c0e9c5-c5da-4f84-b3a8-7568fddc9922','allow-default-scopes','true'),('28a4890f-baf6-4a6a-a659-64836e8e73e1','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','oidc-sha256-pairwise-sub-mapper'),('29a0d84f-2827-48da-8fdf-0684079850f0','a5c75855-4661-4655-b4b0-c0fb8514742a','algorithm','HS512'),('2b98237e-5ebb-4d4a-b423-3cc238a58b48','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','saml-user-property-mapper'),('2dd313fd-8f1c-450b-afe8-94cea99bcaa1','42059290-819f-4c0a-8ad7-d05a5d9688ba','keyUse','ENC'),('2e0e3117-f223-49cd-a9b7-7b491ed9a502','50f6a4e5-e3d2-44b3-82f6-437e29721804','privateKey','MIIEpAIBAAKCAQEA2fjCFXNpZ04Y98oY0HdEKBG+FTwHJzEB/1/ZKzyB2ToK0hf0+me+H/EGkziztZbUA8XrMR6dilUfKTdvXCCNaTsIlmsu+lHfz0C3jzWGAebrc5f84SbstsDqRza+Rh7GcYHW6/JspiHOv8ejeAzq2YCHUR13OgxFsfFp3ozsj0CdRqLVnhnXwgPSWKviQd9Ae2HtL+O/Gm8J13qOojUiJr4hEAcMXcKQtlAZv6/vqB+ggiXW5EEmy/yehwbRa3Rx9dMT1OtW5sz8kpNFozqpb9BsM3lg/J+9e6ydW4jUOKlX0QxQoaafmSayREKiNWxv7WNbxdNfR0WSyy6bhUc5dQIDAQABAoIBABGRTQMxlBMxZi3CahGTKzkXhPxtJB5ygri0XupvK//3/5tDNwlfhyFH6cG/1iwShe/v6a7Byuy1Q5es0hrYvMz4Zw7IgLbIFoO7sY+nPq+0jtF4NHdpDbcFAVzP+rHoFRJc5peJqFwTaXhnfLNWRy9UfpN5DnztNEjNLj0zmnVJF79qEIhDnlxjqINqdjE9/yCKWKZWKcgKPcT7sFwonx32rhsZsVisTlmvj0xguRlnSeC5ESyPcBqQn4op6dPOG2RdrWaK/FEciOPOXys6wiTZa6D61Ci7tg9EWLlNUr8aDGN0giI1tCi9gwoEM2O55BjOB3Y6eYKdReW7sqdbFoECgYEA+nU4Yo++6QLfCAW+I53Y0FkeSHjYXxBn0xxiaoLtT0XyQ+aUa/vS0VFxYdsUcoswhU1qim5ve+wdIe9CQHEWh6vwo16+40nidGb5nqmPegyVsPz2Ee4aYH1MB7T/+RYA3oVme20TpVTMuW68vjkoviJX1J9kHg9tHaldgvwMfFECgYEA3suDErBSqbrgsxraqvpJTcsloilizZBHfVXsKlVSU94XkLFCuE6p6AJj9BdmM3AVbfx290Yx/JbAE0kVsxyvLxaOHnIVb9PuzAHCzS+p9JF80QDjBf2QYAxQ4cKLwvp9RDNGLg7gBX1dLnX74wUF9O8ZFeuAnlFl26NvkADbdeUCgYAOpfZzRUb8J8/Vj0hsmtnIbb3orTCydqor3bgf1KJ+hW/C6uR5YFQXK/U+rOqIPH3M/hciG41nqQqXJoAwdqzDqJm9ZFgzwOl+t1sYHYCnub6ziEf4Cn3+cLWWqc5iyheo7BJvW5TPkaMboU9mrL8mR6ntZJkdbPeLyZEPjSBJ0QKBgQDQM2bdxCC7tIPfXC0UsqhmJn2YuQw4BaiJHA6C0REKhvWGpeAv1HM6+WDm7ib30rpz+gZCtjpoTYhZfWbl5txfKLjPAtW9l1QFGXAtx2kPwBXgSu/c3URaPagoEWICZ4tAfJoM2KHI/4pgrKkjUcJrTjUnItkjGfYR9v1+mQ59cQKBgQCjIo7RcJM257UTGfzLs5k2D0UqzbuqGaIG/SddN856tJYV1qLNse/h/YWa4nDfDpRAlMudJbJGjcSXeIKjgv/Mm9pD6t1h/ukkcLeGZv5I8KmXyuh6my6ld0GxxT+a2QMtENlmiTjRs6SADrKXRiJndofl8Q4TuBDAlmOM7/3xHg=='),('2eb643ad-b5a1-4ed3-a14c-8c7e7e1048b3','22cf7c67-4e40-490e-890a-1e257f4d8eb3','priority','100'),('2eed2889-c4ca-47d2-ba9c-5b7f7d453ecf','88604e8a-c8d7-40af-ba77-17969f2190ff','privateKey','MIIEowIBAAKCAQEA1P/ESrVaDxPMVcHgHaCqdHqfT6PdE5x/D7ungcxyXK459rAePBpXauAFq8P4thbPumitddh3M2noVWMJ53DSk1vpRXG4RppgcyX1oeAuxKiZAFNETVGS9sDRWC3sDGrojYjJIZzkAaPpAysIUPRr7h+xlX2W/IRyjhlZ0uq9wWDIJLW/mxmLIOncS+JMqQeuk/w47e9YxUK9mmiDwvEoKDb3bm1YJOFr3XBA7l6SAu0SelW4/z/NhzxewySKxxaKV/0vhPF8mwtia/0mS3AXf1kmHPox+IryYC3uLr9RtQHSYXxw97EXojl5UHdUcicIBonJzvwv1zery3v2nVxfPQIDAQABAoIBAAF0uDghgyjUuWWlOXoJer/ozFVBuRHxVLbm2CrbqRiSXNx0yclz4vmyEnxLuXBMh+V4jIlupv994xXBOL8jHFFftgtLOclubW7112GgojIYNpmFuC9/AKqVvG+0gcO/L+MIcxO7DB3T+duv4U9Bdq2DfDmEc3IQfO0xdBzP1JIqVb1VsYIAU0eMicX2ZIiZX+R9inNe4u3jX+qVDhz6c9RVBTAy8KhHcWsweIaAg/J7U+JTVvzF8PUz2UWmSgZFmoXnr7IUwrebbLlVd+tAyPqaQ83+2uIxzheAcdQYNYDkaHSNtwTGP/qdL4dFWFbmVtYjA9D6j6vt0C4ukvcFfpkCgYEA/RkGciwfSLGocJiCtS0TK3WlYfbog/55ZFIbEBIzBNiVHXolRuCdfN1iGUdKYdljA+PJ0R38lzfDvi0xSaziHsW929ctsDx0jZo8eL64P9/44uTYtR5iZ02D7X+jR8NAm4Xc0YNmp6TXssHkIRm0Crn/9mCanamvvn3bTZREnhUCgYEA13EH6/eOvkKYVlFqUya7g28MiRL+mIVGhA5Eg6gJuontlyh7M0lRxhi9L5bdMqf2I+4vmCC8X5R9pGFvjzMFoTRgOUlGko12n8+okE9oQqVRA1Ya26Omf6CGajlFchltQ7S+/wmnjWsm3Xzqgg0/R6KAbG2sXNzaI7C59XpMLokCgYBwSPDuKBr88ixrP44FeOjfSANGMfc3z+blFfQBrkFKLhgBitYM2sujwMertVAPlxTxfdZkWuxb3haOQZhzOirbQhkX9h/D5x5CUCHx45L0a5YMe0CVIdTARch4zj6PMNVx6fJsVOJvD5fOK2zdLn0MoTYohPUzazuXxFhN07CZfQKBgQClexGNozpGU9q1OpYDxpVzcEHdq/RY7M3sMxVXVyQZ30XavGJdU2z3TxtJyYSgGzlKAfMW09Y9SE51i83n3VXuq89ZUTpe6T/7osKkDgpUXqzqdiHtITeLgp6cSt9Xs8Ykolmuhhjn3C0fJYrvWAOKH3yrTFZ/gbggf+dmcYjAmQKBgDy159OEH11Bv6+yY6Rd1Af7BvCUFoo3PWN9OjQa3K7BsCox3vvko+ZrZobdO7YKJdTpM/vm4Z6Wc3Ia07sNZ24+k/vEnxz6GwWNnDvYWxRfD6VoA3/lZsNXFpabTqdj0m0570YgEew6JGFhpoVLMPvJXBhG2FFExBHeXSp3c3sZ'),('2ffa1794-f449-4272-86d0-adfb63c03a18','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','saml-user-property-mapper'),('326b6ff5-81eb-42d0-8122-51f7a0d5db2e','42059290-819f-4c0a-8ad7-d05a5d9688ba','certificate','MIICmzCCAYMCBgGX4AcTlzANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjUwNzA2MTM1NTQ5WhcNMzUwNzA2MTM1NzI5WjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCU+JZ0RLke59ApzPq/I0lCukBghFntlGXm/+85MK4eHiFyssHvYs1aD+US0poKx3+14wImagYQyOwEt7zITww41vsKEgviXfLHo8GlZEFS1pX7bSWbxeu7l1b6SvQKHDJ63AMd8hEAFkoXPOa8f2IggoTdG40UAArvDCPn4mFW2CT6NCxZd4+njMadOS+FmAUX53MDHNaIn6HkSqF/EgMdGpSqfsWghkhnhefkcQFesyhbNYepCEaLMAKxfJzmXU/GJKJBsV0sOcals0JLMey0B5pQTkHGkeVECPV2ldEpPTZidjAF8I5dPo8hEGqWfqopLXAhtFPh7mx8BVE4yqAJAgMBAAEwDQYJKoZIhvcNAQELBQADggEBACnBUP/W+GESnvfYnHNH6RV84CLhMBIfVp2ikhZC+rU+a5GPGOkEPbaqPxbc8IplmqkzC5gsa34Rxt+P1/ajM6IhW8l8wufkI8nxmvZ0OGFDzOEYMpHY8K6Qj1WIhYho17hjlXG/nlm82BUayB9rX++EPxdzvz37rmEpWETGPM07ZhtCoTYQ/DDAhOwDOkY5+T6A7ijVUTRYU34V2tsJNV+263rKqmdtTa0jf65kiOgNxKch6W5yB+Z9VAKvZ3AiUjaTkKkkMgfRke2If2rqjw2FO3pYJyDjuGAbDQ0+d8CHqkOm2NeXSb+9GxVSC8GQYJkvmQ+u9ObIaxd8w9DUTNk='),('328db207-5126-4f04-aa08-7b6987bb158d','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','saml-user-property-mapper'),('35f944e3-0cc5-4e85-8e17-78b7054bcabb','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','oidc-usermodel-property-mapper'),('384cde86-dd85-4524-822c-bb32dc0838e6','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','saml-user-attribute-mapper'),('3942cc55-3d8c-461a-9d71-59ceea45a62c','bb63ad2b-b90a-4405-9f2e-e6ce04f9cfa1','client-uris-must-match','true'),('3a844b03-ed56-457d-b0cc-72ec367c36cc','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','oidc-sha256-pairwise-sub-mapper'),('3a87999d-ee88-458b-a429-b1be7f3c4e9c','bb63ad2b-b90a-4405-9f2e-e6ce04f9cfa1','host-sending-registration-request-must-match','true'),('3adda302-f535-4962-8f67-c97c1d423b37','a5c75855-4661-4655-b4b0-c0fb8514742a','secret','Szo8GK2ZyZITBHx_l_zMWJJMQXXJvgZ1fCll2-6hzfocOLWlvSeknEyFji8z-uQLkPKp7jYQHQQ16SSfUM2TbrKPXA83Bgjy7KaxFmTPqmCH3oJQqWmVyu4f8wqWhNDYDMyA1B3Y1qz2chyLQiMeCWQhIfYRMSAjPBCgdiu48CA'),('3e07cf5e-f343-4af0-99fe-a9c9e5bcb709','92fde300-ed76-454f-8fd9-4659ca2329a1','allow-default-scopes','true'),('3f1ee812-eedc-49a3-8310-89cf9b4887cb','361ebca1-f1d3-48cc-9836-b82bb962d15c','client-uris-must-match','true'),('403110dc-1316-47d9-ba24-fe68ba20db33','cf8950f9-fdf3-41e5-abd6-18af017ad108','max-clients','200'),('4eff4c48-fad5-4429-94b5-e0172a9826ad','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','oidc-usermodel-attribute-mapper'),('52c5e86a-aeee-4581-ae1f-af5683a1c58f','22cf7c67-4e40-490e-890a-1e257f4d8eb3','keyUse','SIG'),('5510a82b-6140-4326-ac13-a14e6ba966b2','d94199c3-206d-4b00-9c87-5d956182b536','kc.user.profile.config','{\"attributes\":[{\"name\":\"username\",\"displayName\":\"${username}\",\"validations\":{\"length\":{\"min\":3,\"max\":255},\"username-prohibited-characters\":{},\"up-username-not-idn-homograph\":{}},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false},{\"name\":\"email\",\"displayName\":\"${email}\",\"validations\":{\"email\":{},\"length\":{\"max\":255}},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false},{\"name\":\"firstName\",\"displayName\":\"${firstName}\",\"validations\":{\"length\":{\"max\":255},\"person-name-prohibited-characters\":{}},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false},{\"name\":\"lastName\",\"displayName\":\"${lastName}\",\"validations\":{\"length\":{\"max\":255},\"person-name-prohibited-characters\":{}},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false}],\"groups\":[{\"name\":\"user-metadata\",\"displayHeader\":\"User metadata\",\"displayDescription\":\"Attributes, which refer to user metadata\"}]}'),('58f824df-bb44-4700-bd64-5f557486c194','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','oidc-usermodel-property-mapper'),('593fdca3-e0eb-429d-97b8-96b9adcb4402','42059290-819f-4c0a-8ad7-d05a5d9688ba','priority','100'),('5d9da2f7-38aa-4aab-b691-75d99ff59933','3fceb816-0a49-46eb-b4e2-3b410f0d08cd','algorithm','HS512'),('5e7c3727-64b1-41bd-8754-656e114e9461','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','oidc-usermodel-attribute-mapper'),('6add8f5b-2343-458b-9f53-0dafde41d681','a5c75855-4661-4655-b4b0-c0fb8514742a','priority','100'),('705395fa-daa9-475a-a580-f4fbb6325e41','361ebca1-f1d3-48cc-9836-b82bb962d15c','host-sending-registration-request-must-match','true'),('717590d1-dbf9-48e5-b21b-afd9e4e010b8','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','oidc-address-mapper'),('7a1dfb96-f79d-4822-b6d7-42a1b082a9f0','42059290-819f-4c0a-8ad7-d05a5d9688ba','privateKey','MIIEogIBAAKCAQEAlPiWdES5HufQKcz6vyNJQrpAYIRZ7ZRl5v/vOTCuHh4hcrLB72LNWg/lEtKaCsd/teMCJmoGEMjsBLe8yE8MONb7ChIL4l3yx6PBpWRBUtaV+20lm8Xru5dW+kr0ChwyetwDHfIRABZKFzzmvH9iIIKE3RuNFAAK7wwj5+JhVtgk+jQsWXePp4zGnTkvhZgFF+dzAxzWiJ+h5EqhfxIDHRqUqn7FoIZIZ4Xn5HEBXrMoWzWHqQhGizACsXyc5l1PxiSiQbFdLDnGpbNCSzHstAeaUE5BxpHlRAj1dpXRKT02YnYwBfCOXT6PIRBqln6qKS1wIbRT4e5sfAVROMqgCQIDAQABAoIBABM+JyUeqXD6DbQQk8Uz2CdD6sPs5LbhJ+aWYb7X8G3Yg4aO26lB8He1POhZBufkjDUg/SqcIGtzoLUdTMftGg2Pi37qBNn5O76ZAw8Lq7p5GIeROj3sa35gVq5fFzXlHYosOETae2Kw/mIRDlcD91dutJ3hDLCsLr1hKpQt9Ffkwi3el1Z/2fYwtIqPOR+XV0fIgz790gw7MkYxFCCk78pTQwiE4k5XVrHr9t0w2yMrSftOtwauiuSNFiRTTUkfEfXBGqovteDcFcLvlMP7wKREg2HurulOI93MLkA2MqqZdBVuEXTmq+T0UvdSbBjeG9QkFGJv6IdNYD+XXKClAd8CgYEAyI7hRyb2S17acxUh/r0+ka2oS2/vYeMUX++gkJ62PRIFIGigSF0s2TkLxZfsshMRjRzDuw6cg8kZhD2EZnKAy7asprAWkJwy5L8dO5F2mpAzlHerU6X34aRFo1+PuKnZ4njCujtpDNrxyIjYfu//kNJ6/B7Xjs3kyHnb98lf7d8CgYEAvib8y24DJn71OeUWP2EUXtzozWIBMpmfM3V2a7oWW9ikKa0PA+quHxT+p2HUZDiVAbnGJQtaAeWyURw2s3vZZg1ZlBPO9jd0MqB2nZPmg/XbrynvjIbfgFo5SXWlxMSihk/g8fGoENwXlde6BGDU3rHrhBk7PemZIThCHA/p3xcCgYAFnMn9HcACt/LRCaRb5pvbo8pz4fwG9z3YuXH62hYre1edYpAptkHNCmWj7+51xUbWdhmcIHqMzZHjO317CHVGPiRyvbbD8TsTX63HvvhctvBalUa8HobjT5+LRN9WwZoRVmPMpmD4NSBTBlFYrAqw2NMMPyi1HFnsqkxXkNEYbQKBgF10p/YLoby/1SKZNy/7NJnTFRAsRM5rtlUCPCuCEA8gEL7y8VhJdT6NgQMfD9YaARISDhKS26/nBpbauP/JtPV1RjuC6MsWQiPHvcSAtmDQ6yHSCIwWla5tqY4zJj/pLL+6bqI61RFEOstIdYJFXRn+c/Q+umEZeEtdbRCBub4NAoGAbsKvQr5i0QesFWrmYoggatswCbvihsONK8KQgDvBuamr/++1mipWfPvZic7DwAcz+/yYORmpIsTbVjZu6eGkcL3An8Jk1nsyenRdzHHQxo0NK0/qZYL65wJcFo101luhLfXawn+uDGjx2DQq7xVcPA2dDNni1BEUA5i/6mfnk2I='),('7df2a292-7436-447a-bf83-702378b6907d','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','oidc-full-name-mapper'),('7f75ef9f-9b75-4696-9f29-f2817453b5f9','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','oidc-usermodel-attribute-mapper'),('82a2f94b-1456-43cf-91f4-ada896d12f48','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','saml-user-attribute-mapper'),('869f43fd-2c9d-404c-b86b-7d57f927a4c1','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','saml-role-list-mapper'),('87113173-601e-4a4a-b83c-654868d04b7a','22cf7c67-4e40-490e-890a-1e257f4d8eb3','certificate','MIICmzCCAYMCBgGX4AcQKzANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjUwNzA2MTM1NTQ4WhcNMzUwNzA2MTM1NzI4WjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDmYE4qutSdracb7d+2wzyNCQ6bo2fsjuYq/3oZ2NUQVDkoVr/2+HLejAYDXgsreNYUPGFttrp9aF8Ucs0bX/0335FG0b8n4kvoRvXAPSX8JIVG/E2E+NZ3s6bbH8nyPNVBtg5+gtHZIfhBJcTExTIY0KTbBfEhUs+VNfbwZUGj0Z7L/5AhGlBFiip6f4de1G5sAJDFD04ysX/wW/9651J7dsq5n7QRcz46IFWtpplCFRn6gKPbDJskuBKdz7MHO21M1EOu/4ZjxhkteRQOdKnJt8BaylhIyYlCX2fSTZpbkJDOTydehoh5vTvgyNYw5dpWCvlehi7sNeFLJbl/AosFAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAGowW2xRyAo1DQvfPwAe0dyNtbK9AjHquKlo8Wc9LYjy233+y0Zexr/t6M5gFjZqaFdqNUyIEtIVLJzY9pk9Jg/Xbcb7k8A2Fi+4u/K2UpMuWilr4xQn5So56LTTaEj5NAdqJr7JAjGHVfOqPPyoSeX/QjxTVALsitkslFbtIG+q4KyjQl2x6dgv8cULz/74F/0ZdLPNkG7LeuHly3vaVWBk2O9VECSgTSjMQPjmjDVJCcF2jvD3PCShozNLRJobcBV585qMvN5zpG85ps1Z4FstLy2xt2BspnIwMyaL1tZAZWmuG2tF+hywHwlgFiYA+o3LfJLkSXhoXXHPqcRN0BY='),('88ef6765-628f-4cec-b88a-f88f60510a00','392c75f9-c9db-4c97-be1a-f9f09e711e3c','priority','100'),('89214bab-6978-4b20-9070-7ba2104597f0','7fc12977-b66a-4914-b20a-03bc53aa6338','max-clients','200'),('8bbcff91-3978-4f32-a230-f4dbf0b61b91','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','saml-user-attribute-mapper'),('8f91cd56-581a-4b35-a11b-239678167cea','88604e8a-c8d7-40af-ba77-17969f2190ff','certificate','MIICnzCCAYcCBgGX4DmXAjANBgkqhkiG9w0BAQsFADATMREwDwYDVQQDDAhrZXljbG9hazAeFw0yNTA3MDYxNDUxMDBaFw0zNTA3MDYxNDUyNDBaMBMxETAPBgNVBAMMCGtleWNsb2FrMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1P/ESrVaDxPMVcHgHaCqdHqfT6PdE5x/D7ungcxyXK459rAePBpXauAFq8P4thbPumitddh3M2noVWMJ53DSk1vpRXG4RppgcyX1oeAuxKiZAFNETVGS9sDRWC3sDGrojYjJIZzkAaPpAysIUPRr7h+xlX2W/IRyjhlZ0uq9wWDIJLW/mxmLIOncS+JMqQeuk/w47e9YxUK9mmiDwvEoKDb3bm1YJOFr3XBA7l6SAu0SelW4/z/NhzxewySKxxaKV/0vhPF8mwtia/0mS3AXf1kmHPox+IryYC3uLr9RtQHSYXxw97EXojl5UHdUcicIBonJzvwv1zery3v2nVxfPQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBSLf2kxcggXwrTSaqeLXlCVibwU0sttZuoXnc1q2FB0PylDCLdqIUVgIYGLz3pYop9jjKUe0w6R6DTVDncR2M0I0dg06Ig1+udGhUai2pQzWnTyWaZK6qaln48o5QRh0oR2rC8gB5FbwJRhUlmOsonAtSGft/5eW/i3H+87Tl2OnawBliGxyI9BrHCZDz9HSipr3bc+B1qoR7yN8Df01Nktv8QsfqI7MkTPtKGNjx8fvdqnXxOkgE4Ks4rxejlDMzRwZRabr91ZgZpq9dE5nDnhqyuPuuKvQU5UMnUqOnzrQ2wjV/+JGNaa7EvuculK+kGKiyun++t/u9hlrfgN8iE'),('99a05ba5-26d5-4f11-86c1-878f331e5ce6','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','saml-role-list-mapper'),('9a7e1a6e-d02a-437f-812f-59c2bf48693a','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','oidc-sha256-pairwise-sub-mapper'),('9b4b3ee2-2e67-417f-88ca-a76a807dedbd','50f6a4e5-e3d2-44b3-82f6-437e29721804','certificate','MIICnzCCAYcCBgGX4DmVgjANBgkqhkiG9w0BAQsFADATMREwDwYDVQQDDAhrZXljbG9hazAeFw0yNTA3MDYxNDUwNTlaFw0zNTA3MDYxNDUyMzlaMBMxETAPBgNVBAMMCGtleWNsb2FrMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2fjCFXNpZ04Y98oY0HdEKBG+FTwHJzEB/1/ZKzyB2ToK0hf0+me+H/EGkziztZbUA8XrMR6dilUfKTdvXCCNaTsIlmsu+lHfz0C3jzWGAebrc5f84SbstsDqRza+Rh7GcYHW6/JspiHOv8ejeAzq2YCHUR13OgxFsfFp3ozsj0CdRqLVnhnXwgPSWKviQd9Ae2HtL+O/Gm8J13qOojUiJr4hEAcMXcKQtlAZv6/vqB+ggiXW5EEmy/yehwbRa3Rx9dMT1OtW5sz8kpNFozqpb9BsM3lg/J+9e6ydW4jUOKlX0QxQoaafmSayREKiNWxv7WNbxdNfR0WSyy6bhUc5dQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBr8l+UFjmoJT8Bzuv812LsKZVUedKysSkIk2l8AN7xTzI8RlFDS3tmbsGl3lYoBb21O0QuYP3inlv389JqMdjPRnE/iMsvRACNyn8hXCI8aRI03KOHpc94e5r3shldjVgSCHJCAkgTXdBloCvkQuwfMnaNLC1V6q3CIgt5dVIRO+bquOmJCf0mtDTOIlzB6RuNHUrynNvcplVTonFB8GSr1iG5fjMl0mcBa8rbs2TYrAViwM1ZDJmJ2PLXSw8MJCuwFWnYXSFatpYNRQ7KoqNQW8fbZ4zAnu7d4R0I+V89+GmwXKuLUqEeGSfM7YqwS0NFYs8UxrCinPAfarhhXm4p'),('9ea55b22-f543-44c6-a0d2-281e687e20cb','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','oidc-full-name-mapper'),('a07ba781-2c79-4d14-a309-65e8c8a9d748','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','oidc-usermodel-attribute-mapper'),('a08dc738-23ac-4bbb-814f-3356d4eb14b5','2ab2cd57-9a34-4a7b-93b7-e91997df03d3','secret','rRMAFrggSuQPlkgYYHpgFg'),('a36525e6-3845-4126-bf96-1e66b37e76fb','88604e8a-c8d7-40af-ba77-17969f2190ff','algorithm','RSA-OAEP'),('a43e841e-b07b-4a4d-a456-7f00d148dddc','3fceb816-0a49-46eb-b4e2-3b410f0d08cd','kid','9e7a1543-a896-4443-be8e-404052bb1c70'),('a56d3340-9edc-4550-a11d-9a6f186daecd','50f6a4e5-e3d2-44b3-82f6-437e29721804','priority','100'),('a60f6161-feb7-4086-a4d9-23748426f3f5','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','oidc-usermodel-property-mapper'),('abf1c636-1fce-4d89-af99-eae055d5934a','6acbd477-28bb-46f6-b974-272b8b5db1f5','allow-default-scopes','true'),('b0037742-6385-40ac-a0dc-704d0d67e067','f51cae22-a61d-4702-979b-e9a468b1c67d','allowed-protocol-mapper-types','oidc-address-mapper'),('b5c1e3e9-6918-49b9-8acc-dc6deba8c399','70b49c9e-843d-4515-a308-12a9f605e510','allowed-protocol-mapper-types','saml-user-attribute-mapper'),('c2c093e2-9b23-4a9b-b8fc-e85c9cdbe5df','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','oidc-usermodel-property-mapper'),('c3f1dc77-b8f7-445b-9b40-fe89b379e40a','2ab2cd57-9a34-4a7b-93b7-e91997df03d3','priority','100'),('c67a6c94-99dc-4631-badf-860beaf8ce52','88604e8a-c8d7-40af-ba77-17969f2190ff','priority','100'),('c6ecc3c5-6c3f-45fb-8770-781d830adfa4','0ebc0f38-1c42-4e68-bd83-07d651604117','allowed-protocol-mapper-types','oidc-address-mapper'),('c8011505-fc4b-4b33-bf9a-8ea6c6ca747f','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','oidc-address-mapper'),('d0546a99-6648-46bb-83c8-230cb22b190a','a5c75855-4661-4655-b4b0-c0fb8514742a','kid','642a0485-fc23-41f8-a907-2086091c4c45'),('d4b61c3e-a82c-401a-960f-ff02c5939465','3fceb816-0a49-46eb-b4e2-3b410f0d08cd','priority','100'),('d7d1fd56-5f68-4cb5-b3fc-84103e011022','2ab2cd57-9a34-4a7b-93b7-e91997df03d3','kid','40a50768-0346-4d3f-984e-ab81e68cd435'),('d840f821-3415-4c26-97bd-938b50298152','688b0a51-9926-4e9c-8490-a318b5983837','allowed-protocol-mapper-types','saml-role-list-mapper'),('db85e59c-9593-47f0-a7ac-d3c4f36e3734','88604e8a-c8d7-40af-ba77-17969f2190ff','keyUse','ENC'),('ddf90352-1652-4ef7-9d05-ef8504a48252','22cf7c67-4e40-490e-890a-1e257f4d8eb3','privateKey','MIIEpAIBAAKCAQEA5mBOKrrUna2nG+3ftsM8jQkOm6Nn7I7mKv96GdjVEFQ5KFa/9vhy3owGA14LK3jWFDxhbba6fWhfFHLNG1/9N9+RRtG/J+JL6Eb1wD0l/CSFRvxNhPjWd7Om2x/J8jzVQbYOfoLR2SH4QSXExMUyGNCk2wXxIVLPlTX28GVBo9Gey/+QIRpQRYoqen+HXtRubACQxQ9OMrF/8Fv/eudSe3bKuZ+0EXM+OiBVraaZQhUZ+oCj2wybJLgSnc+zBzttTNRDrv+GY8YZLXkUDnSpybfAWspYSMmJQl9n0k2aW5CQzk8nXoaIeb074MjWMOXaVgr5XoYu7DXhSyW5fwKLBQIDAQABAoIBADyc3BQcAfLWjKApjvHMHnNRDbnfXWl9E4v+mcaRKUXZkwxM/sbhCYAKQU7DgJbYfSLn3A58xEZYnqOHrPVl5P69YMDMUMoSnTwo52HD9OmJRlOt8EtrGSp3ZxBCJeUJImbemqOokcPaTzH82O+Ynpyu0KEbyBynaBxvDWCkk4ehEKajsLWk9MvQbyUGLVTKrnFQB87Y9L0ii3+m3Ee3ChO3jhUi91grep4Ej6RI3uFlo/wJFa/c4ch5OPmngyZaItxtLzrF9Hcz+sAmXa+gYCWciXYVdSPv7R49m1OWyiwSsOuzZ9r9p3L+YMJR/C3HUhM7OyqFvCEMzuWE/iM/X6UCgYEA9Dz6jfNDFHjJyKYWzyaKFCxWyRw3PYvklb8T6IG532jq7Q7dL91GbcpgUc6dmmpcQ+8GdqGDnkwZvH7ua014iPtKHK/pCX36RShrbc1USWvVJQcy5frtCUhEBMfoA7ps9oYLkEhjCyylLKvMrE+ssSFQx66gnw4vU8FS/5B/RrcCgYEA8XhuzNwlFShSaflEthjnj0uyX9L/PI03LWo+IPEU1kBahMzCASabnhlifI/C8l4XRQr4sE5i2h1bsrxOYD1IenvK2+NaBYOFGYPIPnY/KCSRL9e+DycUvO9F5i2B3+3eTH6/CAxkRgSC8Zt5lI5fL27vsgKMmzaZ82VkJ2RjICMCgYBA48Kd3DfCRKU3zgjZWJ/u5Cjanch/bZ95uZ6Mc4rytGlWOlDB2sRPfylAEukEkaWZ1vOcqSTS9d0Qm5i0oYyYuErkfEgp4XKQ1UBVl+wA5clnYRZAy/4WLUEFFCesdedlSA/icZj+6wmnaK2kSjbTiMpxgw/hsl8YnZBznexy2QKBgQDPSDTFAs2KZfypH+ON9b8BUVejznpvvkF5aNRAbvMWIFQT37lCR6QaiWvzxWzxpTeQeySUJBXTapRG0ELvdmv7SL0RGQ8z4HynNGr2w4bfrNQ9H5P1afTxYuy79KCiW40SWVMfU5PYTxMg1/f8QGMRUsfMJUgSDUMGnyjkb9ZvxQKBgQCV2rVMvddse3D2hL+bdIALbWxTsIQE19az96oqWQYMzkiqx8XtJRb7KRpHTvtsDeb4R/AEgVUXhi2dxeV8PrdQtU1F9hHy+teYcPYp9toFfQvkAfR2NFKzEi1+QWvRTUPMQW07ZQ/RJ05BsA7e4lqd/ifuBitgVhAa9a3bQ2fv8g=='),('ff677591-806d-472e-b429-39c06818bb35','3fceb816-0a49-46eb-b4e2-3b410f0d08cd','secret','E00H81cDFNm1VLvQXlQ0_pljSorj9u7jNfoEll54PSO_6pqBTyYJf6iO3IZ38npPv_s7HIlhqT9yUfmNUyNf-RntJ9MvyX-Prt5EYTYjIsaJa5wPKaV08lw4KaapqkcCRoqa-R907Q1bJlMVgGYITrWdKSeIMJ7VQBd2yxC1oHw'),('ff74c625-54ec-4264-ab17-2745337269b1','1defc255-2f5f-4e7f-a51c-e75d28687d06','allow-default-scopes','true');
/*!40000 ALTER TABLE `COMPONENT_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `COMPOSITE_ROLE`
--

DROP TABLE IF EXISTS `COMPOSITE_ROLE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `COMPOSITE_ROLE` (
  `COMPOSITE` varchar(36) NOT NULL,
  `CHILD_ROLE` varchar(36) NOT NULL,
  PRIMARY KEY (`COMPOSITE`,`CHILD_ROLE`),
  KEY `IDX_COMPOSITE` (`COMPOSITE`),
  KEY `IDX_COMPOSITE_CHILD` (`CHILD_ROLE`),
  CONSTRAINT `FK_A63WVEKFTU8JO1PNJ81E7MCE2` FOREIGN KEY (`COMPOSITE`) REFERENCES `KEYCLOAK_ROLE` (`ID`),
  CONSTRAINT `FK_GR7THLLB9LU8Q4VQA4524JJY8` FOREIGN KEY (`CHILD_ROLE`) REFERENCES `KEYCLOAK_ROLE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `COMPOSITE_ROLE`
--

/*!40000 ALTER TABLE `COMPOSITE_ROLE` DISABLE KEYS */;
INSERT INTO `COMPOSITE_ROLE` VALUES ('13a15d8f-e685-4321-a8e9-9b4299dfef76','696c50b7-1ca4-487a-be10-35af1b1edf30'),('25b888c8-7935-430b-8927-9d244a39060b','6ca12eef-2b56-4303-86d8-7d90befe9e87'),('368e8d6a-5b1d-46b0-9b37-2584c3accb8c','f3882302-e087-474c-b21e-1efc409bc5e8'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','02112d05-ab93-428e-9487-845c27a20b1f'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','02c57b11-11ba-4679-b9a9-a3ea45b8f157'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','0393b358-a958-4f83-bc13-e33b7e27a5ee'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','08a0b56f-7647-4340-9622-15a85b1d33d8'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','1c56677f-1a5f-47e7-ac29-cd386bf7765d'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','25180c3d-0961-4107-9462-d73128d2443f'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','42fc327d-f710-4e25-889b-7ebfdb1d9daa'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','45f31d92-6f8a-43d3-82a4-e14da34873b3'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','4de0e63f-abd5-427c-aab2-2ed7f8a58ec0'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','4eb3aa14-3436-4180-8909-aacbdf43659e'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','57efb658-e3b8-409b-8ae9-d406bb04335b'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','5bb9f798-176d-4de0-a595-06e7067df4d2'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','67118b40-69ce-4576-95e2-c215710a7e8c'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','69146af4-f2ee-40d3-a5f3-bf885dc447cc'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','69c0ec5c-4099-4e2d-87c9-637d35ea738e'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','6a28c7be-f203-4d41-8a3a-e4d9b89bd928'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','6b1f87ee-2c31-4de2-967e-c802c8c24b27'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','6bacd7b8-5e28-4ded-9332-cfa9398fd738'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','6be05d3b-a559-498f-bb01-daf2ca0eb51e'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','6d242725-cf40-47db-a7a9-4be50a7d595b'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','7ff5492c-9bc7-43d8-87c4-b719d65547e9'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','82bab180-d568-4a04-a1f4-cfb93bb94599'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','838a91b6-a65d-4539-9d78-30a598916488'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','89b819f5-80eb-491c-a888-63e8dbec7c7d'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','8f69e23d-14c1-4363-a38c-07326bac027e'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','9d2a9df4-8da5-48c3-b575-93347476f3d6'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','a2336e2e-843e-47a5-a16a-2f10216f7d44'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','a58950e1-dd22-42b0-952f-b2b348d48a36'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','a5b6690f-db3f-4772-ac95-07865102c3cb'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','acb63854-a657-42c4-a70d-cbd710bff9e0'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','ad629a28-6aa3-49a9-a10b-5cb07f3c4de3'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','bf1f74b6-8c60-4b33-a88d-e77620bae619'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','c5f9f487-cf44-4c73-9b16-0fde2924bd00'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','c70f977c-1f7d-45d3-a4b1-fecc8757df50'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','d35f090e-11ff-4e3f-ba25-7be1fd082b4c'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','d9245161-e9f0-4479-86e5-92d9c6ef3c83'),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','e8f934e2-5ff2-414b-9ea7-7e2d53556e32'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','13a15d8f-e685-4321-a8e9-9b4299dfef76'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','5424451b-8a17-4a8d-a590-9b4b7ef59010'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','bac7bce2-1afa-4164-a4e1-54d36c788834'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','d5d76baa-b456-4888-aecf-e359a4704f78'),('6be05d3b-a559-498f-bb01-daf2ca0eb51e','6a28c7be-f203-4d41-8a3a-e4d9b89bd928'),('7e1512be-5397-406b-9181-86bd319a6493','bf764633-fad6-4f0b-9ded-68d89eadea28'),('a0ffeee3-63ba-46fe-9c67-0d11938ba6e4','c83a4447-52e6-4451-9737-38b9619c828d'),('acb63854-a657-42c4-a70d-cbd710bff9e0','02112d05-ab93-428e-9487-845c27a20b1f'),('acb63854-a657-42c4-a70d-cbd710bff9e0','08a0b56f-7647-4340-9622-15a85b1d33d8'),('bf1f74b6-8c60-4b33-a88d-e77620bae619','67118b40-69ce-4576-95e2-c215710a7e8c'),('c5f9f487-cf44-4c73-9b16-0fde2924bd00','02c57b11-11ba-4679-b9a9-a3ea45b8f157'),('c5f9f487-cf44-4c73-9b16-0fde2924bd00','42fc327d-f710-4e25-889b-7ebfdb1d9daa'),('e61e14ab-a1b6-47e3-a182-0c80ce0cc23b','06bd840d-d8b2-4262-92e9-efc268fc5bd4'),('e61e14ab-a1b6-47e3-a182-0c80ce0cc23b','25b888c8-7935-430b-8927-9d244a39060b'),('e61e14ab-a1b6-47e3-a182-0c80ce0cc23b','8c5ddd89-6325-4a03-b1e0-f4e8e6073507'),('e61e14ab-a1b6-47e3-a182-0c80ce0cc23b','8eca61e5-db73-499c-869d-063c7f52f4cc'),('efe37c6e-668b-42b6-a496-185f95031b0f','75696880-7f61-45d5-b326-a5e8b021bb9f'),('efe37c6e-668b-42b6-a496-185f95031b0f','8b453d03-a970-4732-b665-70e6266aa38e'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','071a31b5-330c-4931-b8e6-348573d4774a'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','17a79d1e-453a-4599-bc0d-09385ba9c6ca'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','189dc51b-311a-4786-b014-74ab2bff6673'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','368e8d6a-5b1d-46b0-9b37-2584c3accb8c'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','3a2f4883-a668-4a66-b8c0-a774c0facd86'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','3e038d42-14ca-48ee-a4bd-cec4ec558287'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','490aa571-2591-43df-b357-7f1bd4bd0c54'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','59b77fbd-731f-402e-adca-ae5c779e5505'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','75696880-7f61-45d5-b326-a5e8b021bb9f'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','7a25ff1e-92a6-4a52-9b10-1ae0b1284305'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','8b453d03-a970-4732-b665-70e6266aa38e'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','9771e873-0be5-4096-aab2-6a689d4b1796'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','9964030e-8d82-4291-9dee-a23718b69590'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','db9f0d3b-d226-4d74-a911-6b21e171c1d3'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','de10e7a6-8542-4240-91a9-b7323ba3c2c7'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','e5ed0422-2d74-421a-96a6-e667f85a0ebd'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','efe37c6e-668b-42b6-a496-185f95031b0f'),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','f3882302-e087-474c-b21e-1efc409bc5e8');
/*!40000 ALTER TABLE `COMPOSITE_ROLE` ENABLE KEYS */;

--
-- Table structure for table `CREDENTIAL`
--

DROP TABLE IF EXISTS `CREDENTIAL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CREDENTIAL` (
  `ID` varchar(36) NOT NULL,
  `SALT` tinyblob,
  `TYPE` varchar(255) DEFAULT NULL,
  `USER_ID` varchar(36) DEFAULT NULL,
  `CREATED_DATE` bigint DEFAULT NULL,
  `USER_LABEL` varchar(255) DEFAULT NULL,
  `SECRET_DATA` longtext,
  `CREDENTIAL_DATA` longtext,
  `PRIORITY` int DEFAULT NULL,
  `VERSION` int DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `IDX_USER_CREDENTIAL` (`USER_ID`),
  CONSTRAINT `FK_PFYR0GLASQYL0DEI3KL69R6V0` FOREIGN KEY (`USER_ID`) REFERENCES `USER_ENTITY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CREDENTIAL`
--

/*!40000 ALTER TABLE `CREDENTIAL` DISABLE KEYS */;
INSERT INTO `CREDENTIAL` VALUES ('06ba1d75-beef-4477-bbe3-8aaeebbe6bb8',NULL,'password','4030193c-c653-4e74-a4b4-6fd4e06fb4cf',1755975333922,NULL,'{\"value\":\"SN/I+rb1MsESmh4tH4AG2tZQBbNtgdU8bFPqLZJou9I=\",\"salt\":\"1pdrmH76QqKMqhtzY5Xilw==\",\"additionalParameters\":{}}','{\"hashIterations\":5,\"algorithm\":\"argon2\",\"additionalParameters\":{\"hashLength\":[\"32\"],\"memory\":[\"7168\"],\"type\":[\"id\"],\"version\":[\"1.3\"],\"parallelism\":[\"1\"]}}',10,0),('50fb263e-5c96-480d-b004-733c746cb29f',NULL,'password','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc',1755405087652,'My password','{\"value\":\"Z+p0jih2dHT8Ydw8D3Rfj2LOMlRRtVDi7j5y6JRHJuE=\",\"salt\":\"EymKRB6SIjAF/xYI8DZqzw==\",\"additionalParameters\":{}}','{\"hashIterations\":5,\"algorithm\":\"argon2\",\"additionalParameters\":{\"hashLength\":[\"32\"],\"memory\":[\"7168\"],\"type\":[\"id\"],\"version\":[\"1.3\"],\"parallelism\":[\"1\"]}}',10,1),('6030bbc2-e58d-4eef-aa78-863c58676ddb',NULL,'password','c60b0781-0325-4114-bf24-7fcf6ff76cbb',1756031809304,NULL,'{\"value\":\"SPu9y4YqFuV+WTxZfW+y5aMM3JUJdX6ZsUPHp7GibYw=\",\"salt\":\"F5x/8iGCEJhUWbfh8NhpNQ==\",\"additionalParameters\":{}}','{\"hashIterations\":5,\"algorithm\":\"argon2\",\"additionalParameters\":{\"hashLength\":[\"32\"],\"memory\":[\"7168\"],\"type\":[\"id\"],\"version\":[\"1.3\"],\"parallelism\":[\"1\"]}}',10,0),('b33cbcb6-96c3-493a-beb3-7a497aad8657',NULL,'password','2dac9aff-3c68-43d4-8f18-082789a6c867',1755404949083,'My password','{\"value\":\"XoQZjMVIbsOV3URnW3Og/4peshdnbLb17bXKm7Es7y8=\",\"salt\":\"uZ2Dp1lekXzm60U0KzwjEA==\",\"additionalParameters\":{}}','{\"hashIterations\":5,\"algorithm\":\"argon2\",\"additionalParameters\":{\"hashLength\":[\"32\"],\"memory\":[\"7168\"],\"type\":[\"id\"],\"version\":[\"1.3\"],\"parallelism\":[\"1\"]}}',10,1);
/*!40000 ALTER TABLE `CREDENTIAL` ENABLE KEYS */;

--
-- Table structure for table `DATABASECHANGELOG`
--

DROP TABLE IF EXISTS `DATABASECHANGELOG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `DATABASECHANGELOG` (
  `ID` varchar(255) NOT NULL,
  `AUTHOR` varchar(255) NOT NULL,
  `FILENAME` varchar(255) NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int NOT NULL,
  `EXECTYPE` varchar(10) NOT NULL,
  `MD5SUM` varchar(35) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `COMMENTS` varchar(255) DEFAULT NULL,
  `TAG` varchar(255) DEFAULT NULL,
  `LIQUIBASE` varchar(20) DEFAULT NULL,
  `CONTEXTS` varchar(255) DEFAULT NULL,
  `LABELS` varchar(255) DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DATABASECHANGELOG`
--

/*!40000 ALTER TABLE `DATABASECHANGELOG` DISABLE KEYS */;
INSERT INTO `DATABASECHANGELOG` VALUES ('1.0.0.Final-KEYCLOAK-5461','sthorger@redhat.com','META-INF/jpa-changelog-1.0.0.Final.xml','2025-07-06 13:56:42',1,'EXECUTED','9:6f1016664e21e16d26517a4418f5e3df','createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.0.0.Final-KEYCLOAK-5461','sthorger@redhat.com','META-INF/db2-jpa-changelog-1.0.0.Final.xml','2025-07-06 13:56:42',2,'MARK_RAN','9:828775b1596a07d1200ba1d49e5e3941','createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.1.0.Beta1','sthorger@redhat.com','META-INF/jpa-changelog-1.1.0.Beta1.xml','2025-07-06 13:56:43',3,'EXECUTED','9:5f090e44a7d595883c1fb61f4b41fd38','delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=CLIENT_ATTRIBUTES; createTable tableName=CLIENT_SESSION_NOTE; createTable tableName=APP_NODE_REGISTRATIONS; addColumn table...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.1.0.Final','sthorger@redhat.com','META-INF/jpa-changelog-1.1.0.Final.xml','2025-07-06 13:56:43',4,'EXECUTED','9:c07e577387a3d2c04d1adc9aaad8730e','renameColumn newColumnName=EVENT_TIME, oldColumnName=TIME, tableName=EVENT_ENTITY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.2.0.Beta1','psilva@redhat.com','META-INF/jpa-changelog-1.2.0.Beta1.xml','2025-07-06 13:56:45',5,'EXECUTED','9:b68ce996c655922dbcd2fe6b6ae72686','delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.2.0.Beta1','psilva@redhat.com','META-INF/db2-jpa-changelog-1.2.0.Beta1.xml','2025-07-06 13:56:45',6,'MARK_RAN','9:543b5c9989f024fe35c6f6c5a97de88e','delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.2.0.RC1','bburke@redhat.com','META-INF/jpa-changelog-1.2.0.CR1.xml','2025-07-06 13:56:48',7,'EXECUTED','9:765afebbe21cf5bbca048e632df38336','delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.2.0.RC1','bburke@redhat.com','META-INF/db2-jpa-changelog-1.2.0.CR1.xml','2025-07-06 13:56:48',8,'MARK_RAN','9:db4a145ba11a6fdaefb397f6dbf829a1','delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.2.0.Final','keycloak','META-INF/jpa-changelog-1.2.0.Final.xml','2025-07-06 13:56:48',9,'EXECUTED','9:9d05c7be10cdb873f8bcb41bc3a8ab23','update tableName=CLIENT; update tableName=CLIENT; update tableName=CLIENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.3.0','bburke@redhat.com','META-INF/jpa-changelog-1.3.0.xml','2025-07-06 13:56:50',10,'EXECUTED','9:18593702353128d53111f9b1ff0b82b8','delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=ADMI...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.4.0','bburke@redhat.com','META-INF/jpa-changelog-1.4.0.xml','2025-07-06 13:56:51',11,'EXECUTED','9:6122efe5f090e41a85c0f1c9e52cbb62','delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.4.0','bburke@redhat.com','META-INF/db2-jpa-changelog-1.4.0.xml','2025-07-06 13:56:51',12,'MARK_RAN','9:e1ff28bf7568451453f844c5d54bb0b5','delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.5.0','bburke@redhat.com','META-INF/jpa-changelog-1.5.0.xml','2025-07-06 13:56:51',13,'EXECUTED','9:7af32cd8957fbc069f796b61217483fd','delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.6.1_from15','mposolda@redhat.com','META-INF/jpa-changelog-1.6.1.xml','2025-07-06 13:56:51',14,'EXECUTED','9:6005e15e84714cd83226bf7879f54190','addColumn tableName=REALM; addColumn tableName=KEYCLOAK_ROLE; addColumn tableName=CLIENT; createTable tableName=OFFLINE_USER_SESSION; createTable tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_US_SES_PK2, tableName=...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.6.1_from16-pre','mposolda@redhat.com','META-INF/jpa-changelog-1.6.1.xml','2025-07-06 13:56:51',15,'MARK_RAN','9:bf656f5a2b055d07f314431cae76f06c','delete tableName=OFFLINE_CLIENT_SESSION; delete tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.6.1_from16','mposolda@redhat.com','META-INF/jpa-changelog-1.6.1.xml','2025-07-06 13:56:51',16,'MARK_RAN','9:f8dadc9284440469dcf71e25ca6ab99b','dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_US_SES_PK, tableName=OFFLINE_USER_SESSION; dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_CL_SES_PK, tableName=OFFLINE_CLIENT_SESSION; addColumn tableName=OFFLINE_USER_SESSION; update tableName=OF...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.6.1','mposolda@redhat.com','META-INF/jpa-changelog-1.6.1.xml','2025-07-06 13:56:51',17,'EXECUTED','9:d41d8cd98f00b204e9800998ecf8427e','empty','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.7.0','bburke@redhat.com','META-INF/jpa-changelog-1.7.0.xml','2025-07-06 13:56:55',18,'EXECUTED','9:3368ff0be4c2855ee2dd9ca813b38d8e','createTable tableName=KEYCLOAK_GROUP; createTable tableName=GROUP_ROLE_MAPPING; createTable tableName=GROUP_ATTRIBUTE; createTable tableName=USER_GROUP_MEMBERSHIP; createTable tableName=REALM_DEFAULT_GROUPS; addColumn tableName=IDENTITY_PROVIDER; ...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.8.0','mposolda@redhat.com','META-INF/jpa-changelog-1.8.0.xml','2025-07-06 13:56:56',19,'EXECUTED','9:8ac2fb5dd030b24c0570a763ed75ed20','addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.8.0-2','keycloak','META-INF/jpa-changelog-1.8.0.xml','2025-07-06 13:56:56',20,'EXECUTED','9:f91ddca9b19743db60e3057679810e6c','dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.8.0','mposolda@redhat.com','META-INF/db2-jpa-changelog-1.8.0.xml','2025-07-06 13:56:56',21,'MARK_RAN','9:831e82914316dc8a57dc09d755f23c51','addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.8.0-2','keycloak','META-INF/db2-jpa-changelog-1.8.0.xml','2025-07-06 13:56:56',22,'MARK_RAN','9:f91ddca9b19743db60e3057679810e6c','dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.9.0','mposolda@redhat.com','META-INF/jpa-changelog-1.9.0.xml','2025-07-06 13:56:56',23,'EXECUTED','9:bc3d0f9e823a69dc21e23e94c7a94bb1','update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=REALM; update tableName=REALM; customChange; dr...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.9.1','keycloak','META-INF/jpa-changelog-1.9.1.xml','2025-07-06 13:56:57',24,'EXECUTED','9:c9999da42f543575ab790e76439a2679','modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=PUBLIC_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.9.1','keycloak','META-INF/db2-jpa-changelog-1.9.1.xml','2025-07-06 13:56:57',25,'MARK_RAN','9:0d6c65c6f58732d81569e77b10ba301d','modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM','',NULL,'4.29.1',NULL,NULL,'1810196193'),('1.9.2','keycloak','META-INF/jpa-changelog-1.9.2.xml','2025-07-06 13:56:57',26,'EXECUTED','9:fc576660fc016ae53d2d4778d84d86d0','createIndex indexName=IDX_USER_EMAIL, tableName=USER_ENTITY; createIndex indexName=IDX_USER_ROLE_MAPPING, tableName=USER_ROLE_MAPPING; createIndex indexName=IDX_USER_GROUP_MAPPING, tableName=USER_GROUP_MEMBERSHIP; createIndex indexName=IDX_USER_CO...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-2.0.0','psilva@redhat.com','META-INF/jpa-changelog-authz-2.0.0.xml','2025-07-06 13:56:59',27,'EXECUTED','9:43ed6b0da89ff77206289e87eaa9c024','createTable tableName=RESOURCE_SERVER; addPrimaryKey constraintName=CONSTRAINT_FARS, tableName=RESOURCE_SERVER; addUniqueConstraint constraintName=UK_AU8TT6T700S9V50BU18WS5HA6, tableName=RESOURCE_SERVER; createTable tableName=RESOURCE_SERVER_RESOU...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-2.5.1','psilva@redhat.com','META-INF/jpa-changelog-authz-2.5.1.xml','2025-07-06 13:56:59',28,'EXECUTED','9:44bae577f551b3738740281eceb4ea70','update tableName=RESOURCE_SERVER_POLICY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.1.0-KEYCLOAK-5461','bburke@redhat.com','META-INF/jpa-changelog-2.1.0.xml','2025-07-06 13:56:59',29,'EXECUTED','9:bd88e1f833df0420b01e114533aee5e8','createTable tableName=BROKER_LINK; createTable tableName=FED_USER_ATTRIBUTE; createTable tableName=FED_USER_CONSENT; createTable tableName=FED_USER_CONSENT_ROLE; createTable tableName=FED_USER_CONSENT_PROT_MAPPER; createTable tableName=FED_USER_CR...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.2.0','bburke@redhat.com','META-INF/jpa-changelog-2.2.0.xml','2025-07-06 13:57:00',30,'EXECUTED','9:a7022af5267f019d020edfe316ef4371','addColumn tableName=ADMIN_EVENT_ENTITY; createTable tableName=CREDENTIAL_ATTRIBUTE; createTable tableName=FED_CREDENTIAL_ATTRIBUTE; modifyDataType columnName=VALUE, tableName=CREDENTIAL; addForeignKeyConstraint baseTableName=FED_CREDENTIAL_ATTRIBU...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.3.0','bburke@redhat.com','META-INF/jpa-changelog-2.3.0.xml','2025-07-06 13:57:00',31,'EXECUTED','9:fc155c394040654d6a79227e56f5e25a','createTable tableName=FEDERATED_USER; addPrimaryKey constraintName=CONSTR_FEDERATED_USER, tableName=FEDERATED_USER; dropDefaultValue columnName=TOTP, tableName=USER_ENTITY; dropColumn columnName=TOTP, tableName=USER_ENTITY; addColumn tableName=IDE...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.4.0','bburke@redhat.com','META-INF/jpa-changelog-2.4.0.xml','2025-07-06 13:57:00',32,'EXECUTED','9:eac4ffb2a14795e5dc7b426063e54d88','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.5.0','bburke@redhat.com','META-INF/jpa-changelog-2.5.0.xml','2025-07-06 13:57:00',33,'EXECUTED','9:54937c05672568c4c64fc9524c1e9462','customChange; modifyDataType columnName=USER_ID, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.5.0-unicode-oracle','hmlnarik@redhat.com','META-INF/jpa-changelog-2.5.0.xml','2025-07-06 13:57:00',34,'MARK_RAN','9:99205b980a6769e396eca19a4f886c93','modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.5.0-unicode-other-dbs','hmlnarik@redhat.com','META-INF/jpa-changelog-2.5.0.xml','2025-07-06 13:57:02',35,'EXECUTED','9:33d72168746f81f98ae3a1e8e0ca3554','modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.5.0-duplicate-email-support','slawomir@dabek.name','META-INF/jpa-changelog-2.5.0.xml','2025-07-06 13:57:02',36,'EXECUTED','9:61b6d3d7a4c0e0024b0c839da283da0c','addColumn tableName=REALM','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.5.0-unique-group-names','hmlnarik@redhat.com','META-INF/jpa-changelog-2.5.0.xml','2025-07-06 13:57:02',37,'EXECUTED','9:8dcac7bdf7378e7d823cdfddebf72fda','addUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP','',NULL,'4.29.1',NULL,NULL,'1810196193'),('2.5.1','bburke@redhat.com','META-INF/jpa-changelog-2.5.1.xml','2025-07-06 13:57:02',38,'EXECUTED','9:a2b870802540cb3faa72098db5388af3','addColumn tableName=FED_USER_CONSENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.0.0','bburke@redhat.com','META-INF/jpa-changelog-3.0.0.xml','2025-07-06 13:57:02',39,'EXECUTED','9:132a67499ba24bcc54fb5cbdcfe7e4c0','addColumn tableName=IDENTITY_PROVIDER','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.2.0-fix','keycloak','META-INF/jpa-changelog-3.2.0.xml','2025-07-06 13:57:02',40,'MARK_RAN','9:938f894c032f5430f2b0fafb1a243462','addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.2.0-fix-with-keycloak-5416','keycloak','META-INF/jpa-changelog-3.2.0.xml','2025-07-06 13:57:02',41,'MARK_RAN','9:845c332ff1874dc5d35974b0babf3006','dropIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS; addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS; createIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.2.0-fix-offline-sessions','hmlnarik','META-INF/jpa-changelog-3.2.0.xml','2025-07-06 13:57:02',42,'EXECUTED','9:fc86359c079781adc577c5a217e4d04c','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.2.0-fixed','keycloak','META-INF/jpa-changelog-3.2.0.xml','2025-07-06 13:57:05',43,'EXECUTED','9:59a64800e3c0d09b825f8a3b444fa8f4','addColumn tableName=REALM; dropPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_PK2, tableName=OFFLINE_CLIENT_SESSION; dropColumn columnName=CLIENT_SESSION_ID, tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_P...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.3.0','keycloak','META-INF/jpa-changelog-3.3.0.xml','2025-07-06 13:57:05',44,'EXECUTED','9:d48d6da5c6ccf667807f633fe489ce88','addColumn tableName=USER_ENTITY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-3.4.0.CR1-resource-server-pk-change-part1','glavoie@gmail.com','META-INF/jpa-changelog-authz-3.4.0.CR1.xml','2025-07-06 13:57:05',45,'EXECUTED','9:dde36f7973e80d71fceee683bc5d2951','addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_RESOURCE; addColumn tableName=RESOURCE_SERVER_SCOPE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-3.4.0.CR1-resource-server-pk-change-part2-KEYCLOAK-6095','hmlnarik@redhat.com','META-INF/jpa-changelog-authz-3.4.0.CR1.xml','2025-07-06 13:57:05',46,'EXECUTED','9:b855e9b0a406b34fa323235a0cf4f640','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-3.4.0.CR1-resource-server-pk-change-part3-fixed','glavoie@gmail.com','META-INF/jpa-changelog-authz-3.4.0.CR1.xml','2025-07-06 13:57:05',47,'MARK_RAN','9:51abbacd7b416c50c4421a8cabf7927e','dropIndex indexName=IDX_RES_SERV_POL_RES_SERV, tableName=RESOURCE_SERVER_POLICY; dropIndex indexName=IDX_RES_SRV_RES_RES_SRV, tableName=RESOURCE_SERVER_RESOURCE; dropIndex indexName=IDX_RES_SRV_SCOPE_RES_SRV, tableName=RESOURCE_SERVER_SCOPE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-3.4.0.CR1-resource-server-pk-change-part3-fixed-nodropindex','glavoie@gmail.com','META-INF/jpa-changelog-authz-3.4.0.CR1.xml','2025-07-06 13:57:06',48,'EXECUTED','9:bdc99e567b3398bac83263d375aad143','addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_POLICY; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_RESOURCE; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, ...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authn-3.4.0.CR1-refresh-token-max-reuse','glavoie@gmail.com','META-INF/jpa-changelog-authz-3.4.0.CR1.xml','2025-07-06 13:57:06',49,'EXECUTED','9:d198654156881c46bfba39abd7769e69','addColumn tableName=REALM','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.4.0','keycloak','META-INF/jpa-changelog-3.4.0.xml','2025-07-06 13:57:07',50,'EXECUTED','9:cfdd8736332ccdd72c5256ccb42335db','addPrimaryKey constraintName=CONSTRAINT_REALM_DEFAULT_ROLES, tableName=REALM_DEFAULT_ROLES; addPrimaryKey constraintName=CONSTRAINT_COMPOSITE_ROLE, tableName=COMPOSITE_ROLE; addPrimaryKey constraintName=CONSTR_REALM_DEFAULT_GROUPS, tableName=REALM...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.4.0-KEYCLOAK-5230','hmlnarik@redhat.com','META-INF/jpa-changelog-3.4.0.xml','2025-07-06 13:57:08',51,'EXECUTED','9:7c84de3d9bd84d7f077607c1a4dcb714','createIndex indexName=IDX_FU_ATTRIBUTE, tableName=FED_USER_ATTRIBUTE; createIndex indexName=IDX_FU_CONSENT, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CONSENT_RU, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CREDENTIAL, t...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.4.1','psilva@redhat.com','META-INF/jpa-changelog-3.4.1.xml','2025-07-06 13:57:08',52,'EXECUTED','9:5a6bb36cbefb6a9d6928452c0852af2d','modifyDataType columnName=VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.4.2','keycloak','META-INF/jpa-changelog-3.4.2.xml','2025-07-06 13:57:08',53,'EXECUTED','9:8f23e334dbc59f82e0a328373ca6ced0','update tableName=REALM','',NULL,'4.29.1',NULL,NULL,'1810196193'),('3.4.2-KEYCLOAK-5172','mkanis@redhat.com','META-INF/jpa-changelog-3.4.2.xml','2025-07-06 13:57:08',54,'EXECUTED','9:9156214268f09d970cdf0e1564d866af','update tableName=CLIENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.0.0-KEYCLOAK-6335','bburke@redhat.com','META-INF/jpa-changelog-4.0.0.xml','2025-07-06 13:57:08',55,'EXECUTED','9:db806613b1ed154826c02610b7dbdf74','createTable tableName=CLIENT_AUTH_FLOW_BINDINGS; addPrimaryKey constraintName=C_CLI_FLOW_BIND, tableName=CLIENT_AUTH_FLOW_BINDINGS','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.0.0-CLEANUP-UNUSED-TABLE','bburke@redhat.com','META-INF/jpa-changelog-4.0.0.xml','2025-07-06 13:57:08',56,'EXECUTED','9:229a041fb72d5beac76bb94a5fa709de','dropTable tableName=CLIENT_IDENTITY_PROV_MAPPING','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.0.0-KEYCLOAK-6228','bburke@redhat.com','META-INF/jpa-changelog-4.0.0.xml','2025-07-06 13:57:08',57,'EXECUTED','9:079899dade9c1e683f26b2aa9ca6ff04','dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; dropNotNullConstraint columnName=CLIENT_ID, tableName=USER_CONSENT; addColumn tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHO...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.0.0-KEYCLOAK-5579-fixed','mposolda@redhat.com','META-INF/jpa-changelog-4.0.0.xml','2025-07-06 13:57:11',58,'EXECUTED','9:139b79bcbbfe903bb1c2d2a4dbf001d9','dropForeignKeyConstraint baseTableName=CLIENT_TEMPLATE_ATTRIBUTES, constraintName=FK_CL_TEMPL_ATTR_TEMPL; renameTable newTableName=CLIENT_SCOPE_ATTRIBUTES, oldTableName=CLIENT_TEMPLATE_ATTRIBUTES; renameColumn newColumnName=SCOPE_ID, oldColumnName...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-4.0.0.CR1','psilva@redhat.com','META-INF/jpa-changelog-authz-4.0.0.CR1.xml','2025-07-06 13:57:11',59,'EXECUTED','9:b55738ad889860c625ba2bf483495a04','createTable tableName=RESOURCE_SERVER_PERM_TICKET; addPrimaryKey constraintName=CONSTRAINT_FAPMT, tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRHO213XCX4WNKOG82SSPMT...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-4.0.0.Beta3','psilva@redhat.com','META-INF/jpa-changelog-authz-4.0.0.Beta3.xml','2025-07-06 13:57:12',60,'EXECUTED','9:e0057eac39aa8fc8e09ac6cfa4ae15fe','addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRPO2128CX4WNKOG82SSRFY, referencedTableName=RESOURCE_SERVER_POLICY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-4.2.0.Final','mhajas@redhat.com','META-INF/jpa-changelog-authz-4.2.0.Final.xml','2025-07-06 13:57:12',61,'EXECUTED','9:42a33806f3a0443fe0e7feeec821326c','createTable tableName=RESOURCE_URIS; addForeignKeyConstraint baseTableName=RESOURCE_URIS, constraintName=FK_RESOURCE_SERVER_URIS, referencedTableName=RESOURCE_SERVER_RESOURCE; customChange; dropColumn columnName=URI, tableName=RESOURCE_SERVER_RESO...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-4.2.0.Final-KEYCLOAK-9944','hmlnarik@redhat.com','META-INF/jpa-changelog-authz-4.2.0.Final.xml','2025-07-06 13:57:12',62,'EXECUTED','9:9968206fca46eecc1f51db9c024bfe56','addPrimaryKey constraintName=CONSTRAINT_RESOUR_URIS_PK, tableName=RESOURCE_URIS','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.2.0-KEYCLOAK-6313','wadahiro@gmail.com','META-INF/jpa-changelog-4.2.0.xml','2025-07-06 13:57:12',63,'EXECUTED','9:92143a6daea0a3f3b8f598c97ce55c3d','addColumn tableName=REQUIRED_ACTION_PROVIDER','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.3.0-KEYCLOAK-7984','wadahiro@gmail.com','META-INF/jpa-changelog-4.3.0.xml','2025-07-06 13:57:12',64,'EXECUTED','9:82bab26a27195d889fb0429003b18f40','update tableName=REQUIRED_ACTION_PROVIDER','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.6.0-KEYCLOAK-7950','psilva@redhat.com','META-INF/jpa-changelog-4.6.0.xml','2025-07-06 13:57:12',65,'EXECUTED','9:e590c88ddc0b38b0ae4249bbfcb5abc3','update tableName=RESOURCE_SERVER_RESOURCE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.6.0-KEYCLOAK-8377','keycloak','META-INF/jpa-changelog-4.6.0.xml','2025-07-06 13:57:12',66,'EXECUTED','9:5c1f475536118dbdc38d5d7977950cc0','createTable tableName=ROLE_ATTRIBUTE; addPrimaryKey constraintName=CONSTRAINT_ROLE_ATTRIBUTE_PK, tableName=ROLE_ATTRIBUTE; addForeignKeyConstraint baseTableName=ROLE_ATTRIBUTE, constraintName=FK_ROLE_ATTRIBUTE_ID, referencedTableName=KEYCLOAK_ROLE...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.6.0-KEYCLOAK-8555','gideonray@gmail.com','META-INF/jpa-changelog-4.6.0.xml','2025-07-06 13:57:12',67,'EXECUTED','9:e7c9f5f9c4d67ccbbcc215440c718a17','createIndex indexName=IDX_COMPONENT_PROVIDER_TYPE, tableName=COMPONENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.7.0-KEYCLOAK-1267','sguilhen@redhat.com','META-INF/jpa-changelog-4.7.0.xml','2025-07-06 13:57:12',68,'EXECUTED','9:88e0bfdda924690d6f4e430c53447dd5','addColumn tableName=REALM','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.7.0-KEYCLOAK-7275','keycloak','META-INF/jpa-changelog-4.7.0.xml','2025-07-06 13:57:12',69,'EXECUTED','9:f53177f137e1c46b6a88c59ec1cb5218','renameColumn newColumnName=CREATED_ON, oldColumnName=LAST_SESSION_REFRESH, tableName=OFFLINE_USER_SESSION; addNotNullConstraint columnName=CREATED_ON, tableName=OFFLINE_USER_SESSION; addColumn tableName=OFFLINE_USER_SESSION; customChange; createIn...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('4.8.0-KEYCLOAK-8835','sguilhen@redhat.com','META-INF/jpa-changelog-4.8.0.xml','2025-07-06 13:57:13',70,'EXECUTED','9:a74d33da4dc42a37ec27121580d1459f','addNotNullConstraint columnName=SSO_MAX_LIFESPAN_REMEMBER_ME, tableName=REALM; addNotNullConstraint columnName=SSO_IDLE_TIMEOUT_REMEMBER_ME, tableName=REALM','',NULL,'4.29.1',NULL,NULL,'1810196193'),('authz-7.0.0-KEYCLOAK-10443','psilva@redhat.com','META-INF/jpa-changelog-authz-7.0.0.xml','2025-07-06 13:57:13',71,'EXECUTED','9:fd4ade7b90c3b67fae0bfcfcb42dfb5f','addColumn tableName=RESOURCE_SERVER','',NULL,'4.29.1',NULL,NULL,'1810196193'),('8.0.0-adding-credential-columns','keycloak','META-INF/jpa-changelog-8.0.0.xml','2025-07-06 13:57:13',72,'EXECUTED','9:aa072ad090bbba210d8f18781b8cebf4','addColumn tableName=CREDENTIAL; addColumn tableName=FED_USER_CREDENTIAL','',NULL,'4.29.1',NULL,NULL,'1810196193'),('8.0.0-updating-credential-data-not-oracle-fixed','keycloak','META-INF/jpa-changelog-8.0.0.xml','2025-07-06 13:57:13',73,'EXECUTED','9:1ae6be29bab7c2aa376f6983b932be37','update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL','',NULL,'4.29.1',NULL,NULL,'1810196193'),('8.0.0-updating-credential-data-oracle-fixed','keycloak','META-INF/jpa-changelog-8.0.0.xml','2025-07-06 13:57:13',74,'MARK_RAN','9:14706f286953fc9a25286dbd8fb30d97','update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL','',NULL,'4.29.1',NULL,NULL,'1810196193'),('8.0.0-credential-cleanup-fixed','keycloak','META-INF/jpa-changelog-8.0.0.xml','2025-07-06 13:57:14',75,'EXECUTED','9:2b9cc12779be32c5b40e2e67711a218b','dropDefaultValue columnName=COUNTER, tableName=CREDENTIAL; dropDefaultValue columnName=DIGITS, tableName=CREDENTIAL; dropDefaultValue columnName=PERIOD, tableName=CREDENTIAL; dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; dropColumn ...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('8.0.0-resource-tag-support','keycloak','META-INF/jpa-changelog-8.0.0.xml','2025-07-06 13:57:14',76,'EXECUTED','9:91fa186ce7a5af127a2d7a91ee083cc5','addColumn tableName=MIGRATION_MODEL; createIndex indexName=IDX_UPDATE_TIME, tableName=MIGRATION_MODEL','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.0-always-display-client','keycloak','META-INF/jpa-changelog-9.0.0.xml','2025-07-06 13:57:14',77,'EXECUTED','9:6335e5c94e83a2639ccd68dd24e2e5ad','addColumn tableName=CLIENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.0-drop-constraints-for-column-increase','keycloak','META-INF/jpa-changelog-9.0.0.xml','2025-07-06 13:57:14',78,'MARK_RAN','9:6bdb5658951e028bfe16fa0a8228b530','dropUniqueConstraint constraintName=UK_FRSR6T700S9V50BU18WS5PMT, tableName=RESOURCE_SERVER_PERM_TICKET; dropUniqueConstraint constraintName=UK_FRSR6T700S9V50BU18WS5HA6, tableName=RESOURCE_SERVER_RESOURCE; dropPrimaryKey constraintName=CONSTRAINT_O...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.0-increase-column-size-federated-fk','keycloak','META-INF/jpa-changelog-9.0.0.xml','2025-07-06 13:57:14',79,'EXECUTED','9:d5bc15a64117ccad481ce8792d4c608f','modifyDataType columnName=CLIENT_ID, tableName=FED_USER_CONSENT; modifyDataType columnName=CLIENT_REALM_CONSTRAINT, tableName=KEYCLOAK_ROLE; modifyDataType columnName=OWNER, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=CLIENT_ID, ta...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.0-recreate-constraints-after-column-increase','keycloak','META-INF/jpa-changelog-9.0.0.xml','2025-07-06 13:57:14',80,'MARK_RAN','9:077cba51999515f4d3e7ad5619ab592c','addNotNullConstraint columnName=CLIENT_ID, tableName=OFFLINE_CLIENT_SESSION; addNotNullConstraint columnName=OWNER, tableName=RESOURCE_SERVER_PERM_TICKET; addNotNullConstraint columnName=REQUESTER, tableName=RESOURCE_SERVER_PERM_TICKET; addNotNull...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.1-add-index-to-client.client_id','keycloak','META-INF/jpa-changelog-9.0.1.xml','2025-07-06 13:57:14',81,'EXECUTED','9:be969f08a163bf47c6b9e9ead8ac2afb','createIndex indexName=IDX_CLIENT_ID, tableName=CLIENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.1-KEYCLOAK-12579-drop-constraints','keycloak','META-INF/jpa-changelog-9.0.1.xml','2025-07-06 13:57:14',82,'MARK_RAN','9:6d3bb4408ba5a72f39bd8a0b301ec6e3','dropUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.1-KEYCLOAK-12579-add-not-null-constraint','keycloak','META-INF/jpa-changelog-9.0.1.xml','2025-07-06 13:57:15',83,'EXECUTED','9:966bda61e46bebf3cc39518fbed52fa7','addNotNullConstraint columnName=PARENT_GROUP, tableName=KEYCLOAK_GROUP','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.1-KEYCLOAK-12579-recreate-constraints','keycloak','META-INF/jpa-changelog-9.0.1.xml','2025-07-06 13:57:15',84,'MARK_RAN','9:8dcac7bdf7378e7d823cdfddebf72fda','addUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP','',NULL,'4.29.1',NULL,NULL,'1810196193'),('9.0.1-add-index-to-events','keycloak','META-INF/jpa-changelog-9.0.1.xml','2025-07-06 13:57:15',85,'EXECUTED','9:7d93d602352a30c0c317e6a609b56599','createIndex indexName=IDX_EVENT_TIME, tableName=EVENT_ENTITY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('map-remove-ri','keycloak','META-INF/jpa-changelog-11.0.0.xml','2025-07-06 13:57:15',86,'EXECUTED','9:71c5969e6cdd8d7b6f47cebc86d37627','dropForeignKeyConstraint baseTableName=REALM, constraintName=FK_TRAF444KK6QRKMS7N56AIWQ5Y; dropForeignKeyConstraint baseTableName=KEYCLOAK_ROLE, constraintName=FK_KJHO5LE2C0RAL09FL8CM9WFW9','',NULL,'4.29.1',NULL,NULL,'1810196193'),('map-remove-ri','keycloak','META-INF/jpa-changelog-12.0.0.xml','2025-07-06 13:57:15',87,'EXECUTED','9:a9ba7d47f065f041b7da856a81762021','dropForeignKeyConstraint baseTableName=REALM_DEFAULT_GROUPS, constraintName=FK_DEF_GROUPS_GROUP; dropForeignKeyConstraint baseTableName=REALM_DEFAULT_ROLES, constraintName=FK_H4WPD7W4HSOOLNI3H0SW7BTJE; dropForeignKeyConstraint baseTableName=CLIENT...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('12.1.0-add-realm-localization-table','keycloak','META-INF/jpa-changelog-12.0.0.xml','2025-07-06 13:57:15',88,'EXECUTED','9:fffabce2bc01e1a8f5110d5278500065','createTable tableName=REALM_LOCALIZATIONS; addPrimaryKey tableName=REALM_LOCALIZATIONS','',NULL,'4.29.1',NULL,NULL,'1810196193'),('default-roles','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',89,'EXECUTED','9:fa8a5b5445e3857f4b010bafb5009957','addColumn tableName=REALM; customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('default-roles-cleanup','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',90,'EXECUTED','9:67ac3241df9a8582d591c5ed87125f39','dropTable tableName=REALM_DEFAULT_ROLES; dropTable tableName=CLIENT_DEFAULT_ROLES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('13.0.0-KEYCLOAK-16844','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',91,'EXECUTED','9:ad1194d66c937e3ffc82386c050ba089','createIndex indexName=IDX_OFFLINE_USS_PRELOAD, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('map-remove-ri-13.0.0','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',92,'EXECUTED','9:d9be619d94af5a2f5d07b9f003543b91','dropForeignKeyConstraint baseTableName=DEFAULT_CLIENT_SCOPE, constraintName=FK_R_DEF_CLI_SCOPE_SCOPE; dropForeignKeyConstraint baseTableName=CLIENT_SCOPE_CLIENT, constraintName=FK_C_CLI_SCOPE_SCOPE; dropForeignKeyConstraint baseTableName=CLIENT_SC...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('13.0.0-KEYCLOAK-17992-drop-constraints','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',93,'MARK_RAN','9:544d201116a0fcc5a5da0925fbbc3bde','dropPrimaryKey constraintName=C_CLI_SCOPE_BIND, tableName=CLIENT_SCOPE_CLIENT; dropIndex indexName=IDX_CLSCOPE_CL, tableName=CLIENT_SCOPE_CLIENT; dropIndex indexName=IDX_CL_CLSCOPE, tableName=CLIENT_SCOPE_CLIENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('13.0.0-increase-column-size-federated','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',94,'EXECUTED','9:43c0c1055b6761b4b3e89de76d612ccf','modifyDataType columnName=CLIENT_ID, tableName=CLIENT_SCOPE_CLIENT; modifyDataType columnName=SCOPE_ID, tableName=CLIENT_SCOPE_CLIENT','',NULL,'4.29.1',NULL,NULL,'1810196193'),('13.0.0-KEYCLOAK-17992-recreate-constraints','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',95,'MARK_RAN','9:8bd711fd0330f4fe980494ca43ab1139','addNotNullConstraint columnName=CLIENT_ID, tableName=CLIENT_SCOPE_CLIENT; addNotNullConstraint columnName=SCOPE_ID, tableName=CLIENT_SCOPE_CLIENT; addPrimaryKey constraintName=C_CLI_SCOPE_BIND, tableName=CLIENT_SCOPE_CLIENT; createIndex indexName=...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('json-string-accomodation-fixed','keycloak','META-INF/jpa-changelog-13.0.0.xml','2025-07-06 13:57:15',96,'EXECUTED','9:e07d2bc0970c348bb06fb63b1f82ddbf','addColumn tableName=REALM_ATTRIBUTE; update tableName=REALM_ATTRIBUTE; dropColumn columnName=VALUE, tableName=REALM_ATTRIBUTE; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=REALM_ATTRIBUTE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('14.0.0-KEYCLOAK-11019','keycloak','META-INF/jpa-changelog-14.0.0.xml','2025-07-06 13:57:16',97,'EXECUTED','9:24fb8611e97f29989bea412aa38d12b7','createIndex indexName=IDX_OFFLINE_CSS_PRELOAD, tableName=OFFLINE_CLIENT_SESSION; createIndex indexName=IDX_OFFLINE_USS_BY_USER, tableName=OFFLINE_USER_SESSION; createIndex indexName=IDX_OFFLINE_USS_BY_USERSESS, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('14.0.0-KEYCLOAK-18286','keycloak','META-INF/jpa-changelog-14.0.0.xml','2025-07-06 13:57:16',98,'MARK_RAN','9:259f89014ce2506ee84740cbf7163aa7','createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('14.0.0-KEYCLOAK-18286-revert','keycloak','META-INF/jpa-changelog-14.0.0.xml','2025-07-06 13:57:16',99,'MARK_RAN','9:04baaf56c116ed19951cbc2cca584022','dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('14.0.0-KEYCLOAK-18286-supported-dbs','keycloak','META-INF/jpa-changelog-14.0.0.xml','2025-07-06 13:57:16',100,'EXECUTED','9:bd2bd0fc7768cf0845ac96a8786fa735','createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('14.0.0-KEYCLOAK-18286-unsupported-dbs','keycloak','META-INF/jpa-changelog-14.0.0.xml','2025-07-06 13:57:16',101,'MARK_RAN','9:d3d977031d431db16e2c181ce49d73e9','createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('KEYCLOAK-17267-add-index-to-user-attributes','keycloak','META-INF/jpa-changelog-14.0.0.xml','2025-07-06 13:57:16',102,'EXECUTED','9:0b305d8d1277f3a89a0a53a659ad274c','createIndex indexName=IDX_USER_ATTRIBUTE_NAME, tableName=USER_ATTRIBUTE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('KEYCLOAK-18146-add-saml-art-binding-identifier','keycloak','META-INF/jpa-changelog-14.0.0.xml','2025-07-06 13:57:16',103,'EXECUTED','9:2c374ad2cdfe20e2905a84c8fac48460','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('15.0.0-KEYCLOAK-18467','keycloak','META-INF/jpa-changelog-15.0.0.xml','2025-07-06 13:57:16',104,'EXECUTED','9:47a760639ac597360a8219f5b768b4de','addColumn tableName=REALM_LOCALIZATIONS; update tableName=REALM_LOCALIZATIONS; dropColumn columnName=TEXTS, tableName=REALM_LOCALIZATIONS; renameColumn newColumnName=TEXTS, oldColumnName=TEXTS_NEW, tableName=REALM_LOCALIZATIONS; addNotNullConstrai...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('17.0.0-9562','keycloak','META-INF/jpa-changelog-17.0.0.xml','2025-07-06 13:57:16',105,'EXECUTED','9:a6272f0576727dd8cad2522335f5d99e','createIndex indexName=IDX_USER_SERVICE_ACCOUNT, tableName=USER_ENTITY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('18.0.0-10625-IDX_ADMIN_EVENT_TIME','keycloak','META-INF/jpa-changelog-18.0.0.xml','2025-07-06 13:57:16',106,'EXECUTED','9:015479dbd691d9cc8669282f4828c41d','createIndex indexName=IDX_ADMIN_EVENT_TIME, tableName=ADMIN_EVENT_ENTITY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('18.0.15-30992-index-consent','keycloak','META-INF/jpa-changelog-18.0.15.xml','2025-07-06 13:57:16',107,'EXECUTED','9:80071ede7a05604b1f4906f3bf3b00f0','createIndex indexName=IDX_USCONSENT_SCOPE_ID, tableName=USER_CONSENT_CLIENT_SCOPE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('19.0.0-10135','keycloak','META-INF/jpa-changelog-19.0.0.xml','2025-07-06 13:57:16',108,'EXECUTED','9:9518e495fdd22f78ad6425cc30630221','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('20.0.0-12964-supported-dbs','keycloak','META-INF/jpa-changelog-20.0.0.xml','2025-07-06 13:57:16',109,'EXECUTED','9:f2e1331a71e0aa85e5608fe42f7f681c','createIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('20.0.0-12964-unsupported-dbs','keycloak','META-INF/jpa-changelog-20.0.0.xml','2025-07-06 13:57:16',110,'MARK_RAN','9:1a6fcaa85e20bdeae0a9ce49b41946a5','createIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('client-attributes-string-accomodation-fixed','keycloak','META-INF/jpa-changelog-20.0.0.xml','2025-07-06 13:57:16',111,'EXECUTED','9:3f332e13e90739ed0c35b0b25b7822ca','addColumn tableName=CLIENT_ATTRIBUTES; update tableName=CLIENT_ATTRIBUTES; dropColumn columnName=VALUE, tableName=CLIENT_ATTRIBUTES; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('21.0.2-17277','keycloak','META-INF/jpa-changelog-21.0.2.xml','2025-07-06 13:57:16',112,'EXECUTED','9:7ee1f7a3fb8f5588f171fb9a6ab623c0','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('21.1.0-19404','keycloak','META-INF/jpa-changelog-21.1.0.xml','2025-07-06 13:57:17',113,'EXECUTED','9:3d7e830b52f33676b9d64f7f2b2ea634','modifyDataType columnName=DECISION_STRATEGY, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=LOGIC, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=POLICY_ENFORCE_MODE, tableName=RESOURCE_SERVER','',NULL,'4.29.1',NULL,NULL,'1810196193'),('21.1.0-19404-2','keycloak','META-INF/jpa-changelog-21.1.0.xml','2025-07-06 13:57:17',114,'MARK_RAN','9:627d032e3ef2c06c0e1f73d2ae25c26c','addColumn tableName=RESOURCE_SERVER_POLICY; update tableName=RESOURCE_SERVER_POLICY; dropColumn columnName=DECISION_STRATEGY, tableName=RESOURCE_SERVER_POLICY; renameColumn newColumnName=DECISION_STRATEGY, oldColumnName=DECISION_STRATEGY_NEW, tabl...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('22.0.0-17484-updated','keycloak','META-INF/jpa-changelog-22.0.0.xml','2025-07-06 13:57:17',115,'EXECUTED','9:90af0bfd30cafc17b9f4d6eccd92b8b3','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('22.0.5-24031','keycloak','META-INF/jpa-changelog-22.0.0.xml','2025-07-06 13:57:17',116,'MARK_RAN','9:a60d2d7b315ec2d3eba9e2f145f9df28','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('23.0.0-12062','keycloak','META-INF/jpa-changelog-23.0.0.xml','2025-07-06 13:57:17',117,'EXECUTED','9:2168fbe728fec46ae9baf15bf80927b8','addColumn tableName=COMPONENT_CONFIG; update tableName=COMPONENT_CONFIG; dropColumn columnName=VALUE, tableName=COMPONENT_CONFIG; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=COMPONENT_CONFIG','',NULL,'4.29.1',NULL,NULL,'1810196193'),('23.0.0-17258','keycloak','META-INF/jpa-changelog-23.0.0.xml','2025-07-06 13:57:17',118,'EXECUTED','9:36506d679a83bbfda85a27ea1864dca8','addColumn tableName=EVENT_ENTITY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('24.0.0-9758','keycloak','META-INF/jpa-changelog-24.0.0.xml','2025-07-06 13:57:17',119,'EXECUTED','9:502c557a5189f600f0f445a9b49ebbce','addColumn tableName=USER_ATTRIBUTE; addColumn tableName=FED_USER_ATTRIBUTE; createIndex indexName=USER_ATTR_LONG_VALUES, tableName=USER_ATTRIBUTE; createIndex indexName=FED_USER_ATTR_LONG_VALUES, tableName=FED_USER_ATTRIBUTE; createIndex indexName...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('24.0.0-9758-2','keycloak','META-INF/jpa-changelog-24.0.0.xml','2025-07-06 13:57:17',120,'EXECUTED','9:bf0fdee10afdf597a987adbf291db7b2','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('24.0.0-26618-drop-index-if-present','keycloak','META-INF/jpa-changelog-24.0.0.xml','2025-07-06 13:57:17',121,'EXECUTED','9:04baaf56c116ed19951cbc2cca584022','dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('24.0.0-26618-reindex','keycloak','META-INF/jpa-changelog-24.0.0.xml','2025-07-06 13:57:17',122,'EXECUTED','9:bd2bd0fc7768cf0845ac96a8786fa735','createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('24.0.2-27228','keycloak','META-INF/jpa-changelog-24.0.2.xml','2025-07-06 13:57:17',123,'EXECUTED','9:eaee11f6b8aa25d2cc6a84fb86fc6238','customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('24.0.2-27967-drop-index-if-present','keycloak','META-INF/jpa-changelog-24.0.2.xml','2025-07-06 13:57:17',124,'MARK_RAN','9:04baaf56c116ed19951cbc2cca584022','dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('24.0.2-27967-reindex','keycloak','META-INF/jpa-changelog-24.0.2.xml','2025-07-06 13:57:17',125,'MARK_RAN','9:d3d977031d431db16e2c181ce49d73e9','createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-tables','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',126,'EXECUTED','9:deda2df035df23388af95bbd36c17cef','addColumn tableName=OFFLINE_USER_SESSION; addColumn tableName=OFFLINE_CLIENT_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-index-creation','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',127,'EXECUTED','9:3e96709818458ae49f3c679ae58d263a','createIndex indexName=IDX_OFFLINE_USS_BY_LAST_SESSION_REFRESH, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-index-cleanup-uss-createdon','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',128,'EXECUTED','9:78ab4fc129ed5e8265dbcc3485fba92f','dropIndex indexName=IDX_OFFLINE_USS_CREATEDON, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-index-cleanup-uss-preload','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',129,'EXECUTED','9:de5f7c1f7e10994ed8b62e621d20eaab','dropIndex indexName=IDX_OFFLINE_USS_PRELOAD, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-index-cleanup-uss-by-usersess','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',130,'EXECUTED','9:6eee220d024e38e89c799417ec33667f','dropIndex indexName=IDX_OFFLINE_USS_BY_USERSESS, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-index-cleanup-css-preload','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',131,'EXECUTED','9:5411d2fb2891d3e8d63ddb55dfa3c0c9','dropIndex indexName=IDX_OFFLINE_CSS_PRELOAD, tableName=OFFLINE_CLIENT_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-index-2-mysql','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',132,'EXECUTED','9:b7ef76036d3126bb83c2423bf4d449d6','createIndex indexName=IDX_OFFLINE_USS_BY_BROKER_SESSION_ID, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28265-index-2-not-mysql','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:17',133,'MARK_RAN','9:23396cf51ab8bc1ae6f0cac7f9f6fcf7','createIndex indexName=IDX_OFFLINE_USS_BY_BROKER_SESSION_ID, tableName=OFFLINE_USER_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-org','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:18',134,'EXECUTED','9:5c859965c2c9b9c72136c360649af157','createTable tableName=ORG; addUniqueConstraint constraintName=UK_ORG_NAME, tableName=ORG; addUniqueConstraint constraintName=UK_ORG_GROUP, tableName=ORG; createTable tableName=ORG_DOMAIN','',NULL,'4.29.1',NULL,NULL,'1810196193'),('unique-consentuser','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:18',135,'MARK_RAN','9:5857626a2ea8767e9a6c66bf3a2cb32f','customChange; dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_LOCAL_CONSENT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_EXTERNAL_CONSENT, tableName=...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('unique-consentuser-mysql','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:18',136,'EXECUTED','9:b79478aad5adaa1bc428e31563f55e8e','customChange; dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_LOCAL_CONSENT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_EXTERNAL_CONSENT, tableName=...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('25.0.0-28861-index-creation','keycloak','META-INF/jpa-changelog-25.0.0.xml','2025-07-06 13:57:18',137,'EXECUTED','9:b9acb58ac958d9ada0fe12a5d4794ab1','createIndex indexName=IDX_PERM_TICKET_REQUESTER, tableName=RESOURCE_SERVER_PERM_TICKET; createIndex indexName=IDX_PERM_TICKET_OWNER, tableName=RESOURCE_SERVER_PERM_TICKET','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0-org-alias','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',138,'EXECUTED','9:6ef7d63e4412b3c2d66ed179159886a4','addColumn tableName=ORG; update tableName=ORG; addNotNullConstraint columnName=ALIAS, tableName=ORG; addUniqueConstraint constraintName=UK_ORG_ALIAS, tableName=ORG','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0-org-group','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',139,'EXECUTED','9:da8e8087d80ef2ace4f89d8c5b9ca223','addColumn tableName=KEYCLOAK_GROUP; update tableName=KEYCLOAK_GROUP; addNotNullConstraint columnName=TYPE, tableName=KEYCLOAK_GROUP; customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0-org-indexes','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',140,'EXECUTED','9:79b05dcd610a8c7f25ec05135eec0857','createIndex indexName=IDX_ORG_DOMAIN_ORG_ID, tableName=ORG_DOMAIN','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0-org-group-membership','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',141,'EXECUTED','9:a6ace2ce583a421d89b01ba2a28dc2d4','addColumn tableName=USER_GROUP_MEMBERSHIP; update tableName=USER_GROUP_MEMBERSHIP; addNotNullConstraint columnName=MEMBERSHIP_TYPE, tableName=USER_GROUP_MEMBERSHIP','',NULL,'4.29.1',NULL,NULL,'1810196193'),('31296-persist-revoked-access-tokens','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',142,'EXECUTED','9:64ef94489d42a358e8304b0e245f0ed4','createTable tableName=REVOKED_TOKEN; addPrimaryKey constraintName=CONSTRAINT_RT, tableName=REVOKED_TOKEN','',NULL,'4.29.1',NULL,NULL,'1810196193'),('31725-index-persist-revoked-access-tokens','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',143,'EXECUTED','9:b994246ec2bf7c94da881e1d28782c7b','createIndex indexName=IDX_REV_TOKEN_ON_EXPIRE, tableName=REVOKED_TOKEN','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0-idps-for-login','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',144,'EXECUTED','9:51f5fffadf986983d4bd59582c6c1604','addColumn tableName=IDENTITY_PROVIDER; createIndex indexName=IDX_IDP_REALM_ORG, tableName=IDENTITY_PROVIDER; createIndex indexName=IDX_IDP_FOR_LOGIN, tableName=IDENTITY_PROVIDER; customChange','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0-32583-drop-redundant-index-on-client-session','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:18',145,'EXECUTED','9:24972d83bf27317a055d234187bb4af9','dropIndex indexName=IDX_US_SESS_ID_ON_CL_SESS, tableName=OFFLINE_CLIENT_SESSION','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0.32582-remove-tables-user-session-user-session-note-and-client-session','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:19',146,'EXECUTED','9:febdc0f47f2ed241c59e60f58c3ceea5','dropTable tableName=CLIENT_SESSION_ROLE; dropTable tableName=CLIENT_SESSION_NOTE; dropTable tableName=CLIENT_SESSION_PROT_MAPPER; dropTable tableName=CLIENT_SESSION_AUTH_STATUS; dropTable tableName=CLIENT_USER_SESSION_NOTE; dropTable tableName=CLI...','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.0.0-33201-org-redirect-url','keycloak','META-INF/jpa-changelog-26.0.0.xml','2025-07-06 13:57:19',147,'EXECUTED','9:4d0e22b0ac68ebe9794fa9cb752ea660','addColumn tableName=ORG','',NULL,'4.29.1',NULL,NULL,'1810196193'),('29399-jdbc-ping-default','keycloak','META-INF/jpa-changelog-26.1.0.xml','2025-07-06 13:57:19',148,'EXECUTED','9:007dbe99d7203fca403b89d4edfdf21e','createTable tableName=JGROUPS_PING; addPrimaryKey constraintName=CONSTRAINT_JGROUPS_PING, tableName=JGROUPS_PING','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.1.0-34013','keycloak','META-INF/jpa-changelog-26.1.0.xml','2025-07-06 13:57:19',149,'EXECUTED','9:e6b686a15759aef99a6d758a5c4c6a26','addColumn tableName=ADMIN_EVENT_ENTITY','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.1.0-34380','keycloak','META-INF/jpa-changelog-26.1.0.xml','2025-07-06 13:57:19',150,'EXECUTED','9:ac8b9edb7c2b6c17a1c7a11fcf5ccf01','dropTable tableName=USERNAME_LOGIN_FAILURE','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.2.0-36750','keycloak','META-INF/jpa-changelog-26.2.0.xml','2025-07-06 13:57:19',151,'EXECUTED','9:b49ce951c22f7eb16480ff085640a33a','createTable tableName=SERVER_CONFIG','',NULL,'4.29.1',NULL,NULL,'1810196193'),('26.2.0-26106','keycloak','META-INF/jpa-changelog-26.2.0.xml','2025-07-06 13:57:19',152,'EXECUTED','9:b5877d5dab7d10ff3a9d209d7beb6680','addColumn tableName=CREDENTIAL','',NULL,'4.29.1',NULL,NULL,'1810196193');
/*!40000 ALTER TABLE `DATABASECHANGELOG` ENABLE KEYS */;

--
-- Table structure for table `DATABASECHANGELOGLOCK`
--

DROP TABLE IF EXISTS `DATABASECHANGELOGLOCK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `DATABASECHANGELOGLOCK` (
  `ID` int NOT NULL,
  `LOCKED` tinyint NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DATABASECHANGELOGLOCK`
--

/*!40000 ALTER TABLE `DATABASECHANGELOGLOCK` DISABLE KEYS */;
INSERT INTO `DATABASECHANGELOGLOCK` VALUES (1,0,NULL,NULL),(1000,0,NULL,NULL);
/*!40000 ALTER TABLE `DATABASECHANGELOGLOCK` ENABLE KEYS */;

--
-- Table structure for table `DEFAULT_CLIENT_SCOPE`
--

DROP TABLE IF EXISTS `DEFAULT_CLIENT_SCOPE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `DEFAULT_CLIENT_SCOPE` (
  `REALM_ID` varchar(36) NOT NULL,
  `SCOPE_ID` varchar(36) NOT NULL,
  `DEFAULT_SCOPE` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`REALM_ID`,`SCOPE_ID`),
  KEY `IDX_DEFCLS_REALM` (`REALM_ID`),
  KEY `IDX_DEFCLS_SCOPE` (`SCOPE_ID`),
  CONSTRAINT `FK_R_DEF_CLI_SCOPE_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DEFAULT_CLIENT_SCOPE`
--

/*!40000 ALTER TABLE `DEFAULT_CLIENT_SCOPE` DISABLE KEYS */;
INSERT INTO `DEFAULT_CLIENT_SCOPE` VALUES ('736d3e5b-4c46-41ed-9afb-47e727ecab9e','1ae58ee6-26ff-4b6f-84e5-f57d18b92b17',0),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','3ae50474-5f28-4562-baa3-4ee4463c183f',1),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f',0),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','5619f20a-e951-410e-af88-38084f0f498b',0),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','9d152c27-e725-4a7e-9984-d18e62b07911',0),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','b503aa77-481f-4457-ad9b-1c444f7d82bd',1),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','c2f2e082-e87c-419d-af80-8803991286f6',1),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','c5ba5bb8-8431-4542-acd4-f9de2730c7d1',1),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','ceb884c9-b61b-4b39-af8f-e5c63106791b',1),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','d59644b7-45b9-4c40-9004-ad0f4c922689',1),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','e61927e1-ec5d-4137-8617-31b436b5c9be',1),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7',0),('736d3e5b-4c46-41ed-9afb-47e727ecab9e','fdd5034a-7fc4-499e-9eaa-3544210e2dd1',1),('b90e8c11-9258-44ee-901a-1650b6e14601','0a69cb43-9249-465f-8bea-e8097fd91e24',1),('b90e8c11-9258-44ee-901a-1650b6e14601','324d157a-d679-4372-adbd-a8f13aa424af',0),('b90e8c11-9258-44ee-901a-1650b6e14601','33dba1c8-8c1c-4128-b02f-4f10b90f97a2',1),('b90e8c11-9258-44ee-901a-1650b6e14601','44bc3d1e-d675-4a76-b1a8-c976fbd843cd',1),('b90e8c11-9258-44ee-901a-1650b6e14601','453424f4-1a6e-412f-ace0-7c92331cc1e4',0),('b90e8c11-9258-44ee-901a-1650b6e14601','5139b7db-f531-479a-91b1-af0146f86a7c',1),('b90e8c11-9258-44ee-901a-1650b6e14601','517304c6-4e73-4a1a-ae1c-89de779c6d86',0),('b90e8c11-9258-44ee-901a-1650b6e14601','6c1afe15-15bd-436c-b923-abfdc3701b4e',1),('b90e8c11-9258-44ee-901a-1650b6e14601','a1327838-8323-46e5-b4c2-85da961df597',1),('b90e8c11-9258-44ee-901a-1650b6e14601','aaa4253c-d77a-4b6e-a67a-2dab2ee49116',1),('b90e8c11-9258-44ee-901a-1650b6e14601','c22b7fda-6a7d-482d-ac0c-8a8301050d83',0),('b90e8c11-9258-44ee-901a-1650b6e14601','f13e4778-609b-484d-affb-3ba1856b32e0',0),('b90e8c11-9258-44ee-901a-1650b6e14601','f420fc2a-382f-4dd0-8d2e-ac376bd22c32',1);
/*!40000 ALTER TABLE `DEFAULT_CLIENT_SCOPE` ENABLE KEYS */;

--
-- Table structure for table `EVENT_ENTITY`
--

DROP TABLE IF EXISTS `EVENT_ENTITY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `EVENT_ENTITY` (
  `ID` varchar(36) NOT NULL,
  `CLIENT_ID` varchar(255) DEFAULT NULL,
  `DETAILS_JSON` text,
  `ERROR` varchar(255) DEFAULT NULL,
  `IP_ADDRESS` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(255) DEFAULT NULL,
  `SESSION_ID` varchar(255) DEFAULT NULL,
  `EVENT_TIME` bigint DEFAULT NULL,
  `TYPE` varchar(255) DEFAULT NULL,
  `USER_ID` varchar(255) DEFAULT NULL,
  `DETAILS_JSON_LONG_VALUE` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  PRIMARY KEY (`ID`),
  KEY `IDX_EVENT_TIME` (`REALM_ID`,`EVENT_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `EVENT_ENTITY`
--

/*!40000 ALTER TABLE `EVENT_ENTITY` DISABLE KEYS */;
INSERT INTO `EVENT_ENTITY` VALUES ('0ba21f2c-eb9f-41a9-a2db-ea3b88c23593','account-console',NULL,NULL,'172.19.0.1','b90e8c11-9258-44ee-901a-1650b6e14601','28528dee-c5f0-4d81-be9c-3de88c325736',1755974349408,'CODE_TO_TOKEN','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc','{\"token_id\":\"onrtac:a90f9614-808a-425f-b2d7-a6510a944db8\",\"grant_type\":\"authorization_code\",\"refresh_token_type\":\"Refresh\",\"scope\":\"openid email profile\",\"refresh_token_id\":\"3281197c-becf-4cf4-85b9-59a199f9c53c\",\"code_id\":\"28528dee-c5f0-4d81-be9c-3de88c325736\",\"client_auth_method\":\"client-secret\"}'),('0bac4f6f-95f2-4263-93e3-9c50b5c0e5b0','doantotnghiep',NULL,'invalid_client_credentials','172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756030279718,'LOGIN_ERROR',NULL,'{\"grant_type\":\"password\"}'),('13d03aa4-8f6d-425d-9e94-3c1fa6541806','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','3954ac1b-0c2b-4128-930e-3ca35cfab237',1756056828696,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:c9dee863-c0f0-4806-a647-e68f60513afd\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"f94d24f2-2513-4a44-b75f-a7a63635411b\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('16fda908-f0f9-4d68-b850-a56f641b72ca','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','03f9d88a-16d4-44ee-b92c-ff888366392c',1756056840067,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:c881d386-3dd6-45a9-8b47-18794ac53d62\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"5aa105e4-089c-4d02-a841-f0aff967313b\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('225032e8-693b-4c5f-8fd3-3a205d1e47f9','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756129014513,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:053d897c-e29d-44c1-af76-55ea43fb4526\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('2a8f70ab-9346-4a7b-b21c-ca9d534829f4','doantotnghiep',NULL,'invalid_client_credentials','172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756030027925,'LOGIN_ERROR',NULL,'{\"grant_type\":\"password\"}'),('2c241753-d2f4-428e-94ec-6b187c0ae2f0','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756135015894,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:e83fd69c-8cde-4a63-90d8-49319f22975a\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('329cc877-4178-4aea-872e-f0c6d7ff296f','doantotnghiep',NULL,NULL,'172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756031808094,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:4032f62e-3d1f-4c82-8373-6861cac3c6eb\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('32e507e7-785b-4cc6-b0ea-0b9faa1423d5','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','7d3ab9d0-e997-42aa-9e79-96c680ceae07',1756056842926,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:3a5d2cf8-50d9-48ee-8a9a-dd3ce5e026f1\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"d41ec64c-e044-42ad-a624-f404fd0302ff\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('362b4bfe-6e67-4b1c-ace9-89e2ff9b50d5','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','5b37622d-784b-408b-9ab8-ef378e1e0111',1756131361618,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:a1d7ebfb-ffee-4fb2-b30a-11d285b60921\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"f058ad6b-ba88-4971-9af2-7bf47a305130\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('3ef2c12a-0884-4dbe-bdbd-35c54ddd2d1c','doantotnghiep',NULL,'invalid_client_credentials','172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756030337882,'LOGIN_ERROR',NULL,'{\"grant_type\":\"password\"}'),('40b91f41-29b1-41f1-a5a5-7a44d3fdf0e2','doantotnghiep',NULL,'invalid_client_credentials','172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756030307901,'LOGIN_ERROR',NULL,'{\"grant_type\":\"password\"}'),('40c64a6a-7ece-4076-8560-f7674255baea','doantotnghiep',NULL,NULL,'172.18.0.2','b90e8c11-9258-44ee-901a-1650b6e14601','31ceea6d-654c-4e45-886f-344e10d35a27',1756033592532,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:a5a096d9-729d-4b38-b921-0564673f407a\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"e555ba8e-2f0c-4fa5-b2d3-035f07a6dad8\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('45981ea5-17d4-4b22-b3f2-88da6b67452f','doantotnghiep',NULL,NULL,'172.18.0.2','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756030028785,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:7be3baea-62df-49e9-9c02-4e1b5f8206bd\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('48535afd-0b6c-4099-9449-e6f8fd4d7797','doantotnghiep',NULL,'invalid_client_credentials','172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756030088374,'LOGIN_ERROR',NULL,'{\"grant_type\":\"password\"}'),('4dff428f-3f07-4b78-a14b-608ea0063311','account-console',NULL,NULL,'172.19.0.1','b90e8c11-9258-44ee-901a-1650b6e14601','28528dee-c5f0-4d81-be9c-3de88c325736',1755974346331,'LOGIN','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc','{\"auth_method\":\"openid-connect\",\"auth_type\":\"code\",\"redirect_uri\":\"http://localhost:8080/realms/keycloak/account\",\"consent\":\"no_consent_required\",\"code_id\":\"28528dee-c5f0-4d81-be9c-3de88c325736\",\"username\":\"dev\"}'),('4fbd01c0-cb3e-4936-b34a-96a90439c7e4','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1755975550879,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:661d0619-ba5f-4bf4-a909-08b4f226a305\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('5131a526-07cb-41e1-9919-454cfcac92b1','doantotnghiep',NULL,NULL,'172.18.0.2','b90e8c11-9258-44ee-901a-1650b6e14601','12b1f13d-4f05-4168-94a7-5d7ff48bb00a',1756033397486,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:4602cc24-3493-4bcb-b350-8a60710675c9\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"8bc6d8b3-6977-48ff-bb4d-a751d8654ff9\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('582bc648-40f1-4afc-a73c-b0b103d86ca8','doantotnghiep',NULL,NULL,'172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756032325636,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:4bf87fc4-8264-41c1-8ff1-cd6808c49886\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('60c9366f-fd0e-4184-b02d-96b1debcddb2','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1755975176690,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:8c7e66a7-80ec-474b-9404-10f2e0e11bad\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('6574b469-9803-4966-874e-e3add0e795e1','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','70ab6c6e-2f8b-4afe-b86f-57fea78f0e42',1756143798327,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:2e4efbd5-53cc-4087-8d6d-f04695fc113e\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"15101e2d-f6a2-4f99-aa6c-66ba68898b43\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('684c4745-525d-4e17-b66a-22b272ea97ee','doantotnghiep',NULL,NULL,'172.18.0.5','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756134574459,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:eea93fca-bcd5-447d-852c-dbb9d767c64e\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('6a8ef16b-f2e3-47de-996e-f73d4da6ebbb','account-console',NULL,NULL,'172.19.0.1','b90e8c11-9258-44ee-901a-1650b6e14601','28528dee-c5f0-4d81-be9c-3de88c325736',1755974348250,'LOGIN','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc','{\"auth_method\":\"openid-connect\",\"auth_type\":\"code\",\"response_type\":\"code\",\"redirect_uri\":\"http://localhost:8080/realms/keycloak/account?session_state=28528dee-c5f0-4d81-be9c-3de88c325736&iss=http%3A%2F%2Flocalhost%3A8080%2Frealms%2Fkeycloak&code=bef59e8e-f878-4aab-98c9-7f6d9020448d.28528dee-c5f0-4d81-be9c-3de88c325736.a57d63c3-4d3c-40d6-a086-9d43cd4fedd5\",\"consent\":\"no_consent_required\",\"code_id\":\"28528dee-c5f0-4d81-be9c-3de88c325736\",\"response_mode\":\"query\",\"username\":\"dev\"}'),('82e0462d-4093-4e7d-8538-50a7e1338853','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','2ac2b761-47d2-4483-9d29-801e060e0555',1756143788417,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:6ba1b60c-93fb-4493-9953-2db9d4c6e166\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"a1df6831-198d-4d5a-b736-b4ec00814935\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('88a0b091-3ae0-41ae-8be9-c050729d7cc5','doantotnghiep',NULL,NULL,'172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601','dabd637f-105d-41ec-be32-2403a1aed1f8',1756031825410,'LOGIN','c60b0781-0325-4114-bf24-7fcf6ff76cbb','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:e22bef03-2833-4b1f-a5d7-d254c3bf5105\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"67b7372a-dc57-491d-9995-4ec16f7b9916\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser1\"}'),('98780259-a72a-428a-a7c7-8be2c31f3106','doantotnghiep',NULL,NULL,'172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756031466673,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:b7104749-25d7-4af0-a252-2ff32ea900be\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('9a5c563c-ebfe-42cb-bc6e-8f18f91c57bf','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','90475dae-5583-49ae-9663-9af53cddc54e',1756137699698,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:1584130f-3ebb-47f2-b9f3-8dab6986df17\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"a8bd2b87-bf1e-45dc-85b9-bf14e19271b4\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('9e1ac096-f207-4b55-8a42-8776eb3fc159','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756137290533,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:88f264eb-2960-4d12-be2f-db352834a5cd\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('a112b122-45d0-420d-b416-82cc39369a90','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','98c7aa6b-146b-46c6-a02e-b612c963fbd4',1756131361618,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:3e47b1a2-a317-4365-bd54-5f8822e564e6\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"61a0a601-fa9f-48e2-b39c-f9e346d9ab20\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('a3464551-b90c-4d94-b39d-123a389967aa','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756131481067,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:d7178b21-db2e-4205-bba1-0b92939fb628\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('adbdc5db-6b0b-4329-b041-ffd627c031b5','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','ad44139e-9cd3-46bd-91d7-6e65adf96147',1756056841499,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:b00905e3-479f-49a5-90a0-d438de35b294\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"66b93fcb-10fb-463c-8ecc-5671e3192eff\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('afbfe449-1df4-4720-b085-8c3420b1196e','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756143722572,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:e91c03f5-280a-42fe-b982-638a5f2c19c3\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('afe50f4d-c608-403d-92f4-0eff20c58e91','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','cde2efec-0eeb-4723-acde-c9cc5d0aee37',1756143814381,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:b8cef82f-564e-411a-a457-641f2122cafe\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"4c05d900-c60e-44e2-8bfd-7f879cfed060\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('ba8def24-0f83-4dee-bcf7-24e56c90156e','doantotnghiep',NULL,NULL,'172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601','dabd637f-105d-41ec-be32-2403a1aed1f8',1756032483160,'LOGOUT','c60b0781-0325-4114-bf24-7fcf6ff76cbb','{\"client_auth_method\":\"client-secret\"}'),('bb0c46c0-5378-49c3-8fac-1b7b1e73b86c','doantotnghiep',NULL,NULL,'172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601','0214e9b4-f157-4cfb-93d1-57c929fece36',1756030331055,'LOGIN','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:548a1db7-218c-4ed7-8ae5-345ec7fe26f3\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"4ecb1d44-dd87-4a0c-a1e4-fadc86ab05b9\",\"client_auth_method\":\"client-secret\",\"username\":\"dev\"}'),('c21eb109-48d0-472c-97f4-54a8badc289d','doantotnghiep',NULL,NULL,'172.18.0.2','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756033367601,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:211bdbea-44ae-407c-95b5-77373c38ddf1\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('c68d5a64-464c-4ac1-b932-6588e3b78861','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','955c0541-4bfd-4ff5-84bd-64e873d48aa2',1756143803221,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:ec92aaeb-4eb6-4736-b000-529c98f47a7f\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"53325814-df60-4306-84b8-b568ac536a31\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('cdac9502-6b27-42b4-bd36-2d3779cdd22a','doantotnghiep',NULL,NULL,'172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601','0e3aac55-b0a2-4999-ac92-dc025fa5b10e',1756030340927,'LOGIN','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:263d25e4-3151-4b6c-b8ab-2a77515470d2\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"71334535-287c-4c1f-b360-e250107da5d5\",\"client_auth_method\":\"client-secret\",\"username\":\"dev\"}'),('cdcb073b-5413-4983-b46a-5a3eee26e7b4','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756137545599,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:1aa598ad-6377-4bf7-999b-21a4e7954f17\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('cdde32c6-d910-4929-8780-89cc50fa30d9','doantotnghiep',NULL,'invalid_client_credentials','172.18.0.1','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756030108394,'LOGIN_ERROR',NULL,'{\"grant_type\":\"password\"}'),('d045f3ee-cf24-44a8-a450-656f49852620','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756056827372,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:93cc5270-f09f-4bb8-8d17-f1195ebb898b\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('d444fdc9-4fe5-497a-b055-4f1a230635f3','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756131317808,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:c50eecce-06a7-48b8-b1f4-9574bf2094ae\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}'),('e237b586-4c92-4031-97c8-1d13cbd1f327','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','678ffd7a-3b47-4256-82e5-0e130abf8184',1756056837299,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:a74c37a2-9712-4cd5-b64a-0439e5dbe902\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"5e3be841-5e30-4329-9677-8b405b0c661c\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('e4f98dd2-5e46-492f-9efe-4e577056a56f','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','a2ed0763-002b-4d60-bb3b-e5cae95fb930',1756143817066,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:34ab152b-36b7-4cb0-8b25-42a9f14ac8db\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"0bee5e71-ea03-4b07-8894-6e88d35eccb1\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('ecf8f0eb-03f3-4188-b4f2-f7eb8741e3e4','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','a46f39d5-fd60-4e08-8854-d774765b3b47',1756102382180,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:2360c811-acde-4307-b2a9-61daaa5018cc\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"2f64d5b4-69bc-4b36-9bc4-5f50172914e0\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('f3af3665-c374-4ad8-89aa-d36ad2809958','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','884c6249-7bd1-45f1-958a-c40b804b169a',1756131523280,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:694704df-4c6d-4489-9257-681bc7066d54\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"e3b7c7a2-08cf-4350-9ced-70415e690268\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('f59a7410-c707-4bdd-b395-5519ba10574c','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601','d5103301-3169-4c4d-bfc0-9d55672fdbd1',1756143810783,'LOGIN','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','{\"auth_method\":\"openid-connect\",\"token_id\":\"onrtro:a94d0478-6aa0-42f3-8d23-e2b887268734\",\"grant_type\":\"password\",\"refresh_token_type\":\"Refresh\",\"scope\":\"email profile\",\"refresh_token_id\":\"d8a01f7a-7cbc-497a-94c7-9ea73852ab6b\",\"client_auth_method\":\"client-secret\",\"username\":\"testuser\"}'),('ff7fa31a-4913-4563-82ef-b5af0e3f3446','doantotnghiep',NULL,NULL,'172.18.0.4','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,1756102298747,'CLIENT_LOGIN','dce7ffce-1455-4041-8f58-224b6d426684','{\"token_id\":\"trrtcc:6fa5705f-414b-4eca-87ff-2961ec9ed4d1\",\"grant_type\":\"client_credentials\",\"scope\":\"email profile\",\"client_auth_method\":\"client-secret\",\"username\":\"service-account-doantotnghiep\"}');
/*!40000 ALTER TABLE `EVENT_ENTITY` ENABLE KEYS */;

--
-- Table structure for table `FEDERATED_IDENTITY`
--

DROP TABLE IF EXISTS `FEDERATED_IDENTITY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FEDERATED_IDENTITY` (
  `IDENTITY_PROVIDER` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  `FEDERATED_USER_ID` varchar(255) DEFAULT NULL,
  `FEDERATED_USERNAME` varchar(255) DEFAULT NULL,
  `TOKEN` text,
  `USER_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`IDENTITY_PROVIDER`,`USER_ID`),
  KEY `IDX_FEDIDENTITY_USER` (`USER_ID`),
  KEY `IDX_FEDIDENTITY_FEDUSER` (`FEDERATED_USER_ID`),
  CONSTRAINT `FK404288B92EF007A6` FOREIGN KEY (`USER_ID`) REFERENCES `USER_ENTITY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FEDERATED_IDENTITY`
--

/*!40000 ALTER TABLE `FEDERATED_IDENTITY` DISABLE KEYS */;
/*!40000 ALTER TABLE `FEDERATED_IDENTITY` ENABLE KEYS */;

--
-- Table structure for table `FEDERATED_USER`
--

DROP TABLE IF EXISTS `FEDERATED_USER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FEDERATED_USER` (
  `ID` varchar(255) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FEDERATED_USER`
--

/*!40000 ALTER TABLE `FEDERATED_USER` DISABLE KEYS */;
/*!40000 ALTER TABLE `FEDERATED_USER` ENABLE KEYS */;

--
-- Table structure for table `FED_USER_ATTRIBUTE`
--

DROP TABLE IF EXISTS `FED_USER_ATTRIBUTE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FED_USER_ATTRIBUTE` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `USER_ID` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(36) DEFAULT NULL,
  `VALUE` text,
  `LONG_VALUE_HASH` binary(64) DEFAULT NULL,
  `LONG_VALUE_HASH_LOWER_CASE` binary(64) DEFAULT NULL,
  `LONG_VALUE` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  PRIMARY KEY (`ID`),
  KEY `IDX_FU_ATTRIBUTE` (`USER_ID`,`REALM_ID`,`NAME`),
  KEY `FED_USER_ATTR_LONG_VALUES` (`LONG_VALUE_HASH`,`NAME`),
  KEY `FED_USER_ATTR_LONG_VALUES_LOWER_CASE` (`LONG_VALUE_HASH_LOWER_CASE`,`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FED_USER_ATTRIBUTE`
--

/*!40000 ALTER TABLE `FED_USER_ATTRIBUTE` DISABLE KEYS */;
/*!40000 ALTER TABLE `FED_USER_ATTRIBUTE` ENABLE KEYS */;

--
-- Table structure for table `FED_USER_CONSENT`
--

DROP TABLE IF EXISTS `FED_USER_CONSENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FED_USER_CONSENT` (
  `ID` varchar(36) NOT NULL,
  `CLIENT_ID` varchar(255) DEFAULT NULL,
  `USER_ID` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(36) DEFAULT NULL,
  `CREATED_DATE` bigint DEFAULT NULL,
  `LAST_UPDATED_DATE` bigint DEFAULT NULL,
  `CLIENT_STORAGE_PROVIDER` varchar(36) DEFAULT NULL,
  `EXTERNAL_CLIENT_ID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_FU_CONSENT` (`USER_ID`,`CLIENT_ID`),
  KEY `IDX_FU_CONSENT_RU` (`REALM_ID`,`USER_ID`),
  KEY `IDX_FU_CNSNT_EXT` (`USER_ID`,`CLIENT_STORAGE_PROVIDER`,`EXTERNAL_CLIENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FED_USER_CONSENT`
--

/*!40000 ALTER TABLE `FED_USER_CONSENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `FED_USER_CONSENT` ENABLE KEYS */;

--
-- Table structure for table `FED_USER_CONSENT_CL_SCOPE`
--

DROP TABLE IF EXISTS `FED_USER_CONSENT_CL_SCOPE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FED_USER_CONSENT_CL_SCOPE` (
  `USER_CONSENT_ID` varchar(36) NOT NULL,
  `SCOPE_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`USER_CONSENT_ID`,`SCOPE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FED_USER_CONSENT_CL_SCOPE`
--

/*!40000 ALTER TABLE `FED_USER_CONSENT_CL_SCOPE` DISABLE KEYS */;
/*!40000 ALTER TABLE `FED_USER_CONSENT_CL_SCOPE` ENABLE KEYS */;

--
-- Table structure for table `FED_USER_CREDENTIAL`
--

DROP TABLE IF EXISTS `FED_USER_CREDENTIAL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FED_USER_CREDENTIAL` (
  `ID` varchar(36) NOT NULL,
  `SALT` tinyblob,
  `TYPE` varchar(255) DEFAULT NULL,
  `CREATED_DATE` bigint DEFAULT NULL,
  `USER_ID` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(36) DEFAULT NULL,
  `USER_LABEL` varchar(255) DEFAULT NULL,
  `SECRET_DATA` longtext,
  `CREDENTIAL_DATA` longtext,
  `PRIORITY` int DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_FU_CREDENTIAL` (`USER_ID`,`TYPE`),
  KEY `IDX_FU_CREDENTIAL_RU` (`REALM_ID`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FED_USER_CREDENTIAL`
--

/*!40000 ALTER TABLE `FED_USER_CREDENTIAL` DISABLE KEYS */;
/*!40000 ALTER TABLE `FED_USER_CREDENTIAL` ENABLE KEYS */;

--
-- Table structure for table `FED_USER_GROUP_MEMBERSHIP`
--

DROP TABLE IF EXISTS `FED_USER_GROUP_MEMBERSHIP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FED_USER_GROUP_MEMBERSHIP` (
  `GROUP_ID` varchar(36) NOT NULL,
  `USER_ID` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`GROUP_ID`,`USER_ID`),
  KEY `IDX_FU_GROUP_MEMBERSHIP` (`USER_ID`,`GROUP_ID`),
  KEY `IDX_FU_GROUP_MEMBERSHIP_RU` (`REALM_ID`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FED_USER_GROUP_MEMBERSHIP`
--

/*!40000 ALTER TABLE `FED_USER_GROUP_MEMBERSHIP` DISABLE KEYS */;
/*!40000 ALTER TABLE `FED_USER_GROUP_MEMBERSHIP` ENABLE KEYS */;

--
-- Table structure for table `FED_USER_REQUIRED_ACTION`
--

DROP TABLE IF EXISTS `FED_USER_REQUIRED_ACTION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FED_USER_REQUIRED_ACTION` (
  `REQUIRED_ACTION` varchar(255) NOT NULL DEFAULT ' ',
  `USER_ID` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`REQUIRED_ACTION`,`USER_ID`),
  KEY `IDX_FU_REQUIRED_ACTION` (`USER_ID`,`REQUIRED_ACTION`),
  KEY `IDX_FU_REQUIRED_ACTION_RU` (`REALM_ID`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FED_USER_REQUIRED_ACTION`
--

/*!40000 ALTER TABLE `FED_USER_REQUIRED_ACTION` DISABLE KEYS */;
/*!40000 ALTER TABLE `FED_USER_REQUIRED_ACTION` ENABLE KEYS */;

--
-- Table structure for table `FED_USER_ROLE_MAPPING`
--

DROP TABLE IF EXISTS `FED_USER_ROLE_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FED_USER_ROLE_MAPPING` (
  `ROLE_ID` varchar(36) NOT NULL,
  `USER_ID` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `STORAGE_PROVIDER_ID` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`ROLE_ID`,`USER_ID`),
  KEY `IDX_FU_ROLE_MAPPING` (`USER_ID`,`ROLE_ID`),
  KEY `IDX_FU_ROLE_MAPPING_RU` (`REALM_ID`,`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `FED_USER_ROLE_MAPPING`
--

/*!40000 ALTER TABLE `FED_USER_ROLE_MAPPING` DISABLE KEYS */;
/*!40000 ALTER TABLE `FED_USER_ROLE_MAPPING` ENABLE KEYS */;

--
-- Table structure for table `GROUP_ATTRIBUTE`
--

DROP TABLE IF EXISTS `GROUP_ATTRIBUTE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `GROUP_ATTRIBUTE` (
  `ID` varchar(36) NOT NULL DEFAULT 'sybase-needs-something-here',
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `GROUP_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_GROUP_ATTR_GROUP` (`GROUP_ID`),
  KEY `IDX_GROUP_ATT_BY_NAME_VALUE` (`NAME`,`VALUE`),
  CONSTRAINT `FK_GROUP_ATTRIBUTE_GROUP` FOREIGN KEY (`GROUP_ID`) REFERENCES `KEYCLOAK_GROUP` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `GROUP_ATTRIBUTE`
--

/*!40000 ALTER TABLE `GROUP_ATTRIBUTE` DISABLE KEYS */;
/*!40000 ALTER TABLE `GROUP_ATTRIBUTE` ENABLE KEYS */;

--
-- Table structure for table `GROUP_ROLE_MAPPING`
--

DROP TABLE IF EXISTS `GROUP_ROLE_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `GROUP_ROLE_MAPPING` (
  `ROLE_ID` varchar(36) NOT NULL,
  `GROUP_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`ROLE_ID`,`GROUP_ID`),
  KEY `IDX_GROUP_ROLE_MAPP_GROUP` (`GROUP_ID`),
  CONSTRAINT `FK_GROUP_ROLE_GROUP` FOREIGN KEY (`GROUP_ID`) REFERENCES `KEYCLOAK_GROUP` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `GROUP_ROLE_MAPPING`
--

/*!40000 ALTER TABLE `GROUP_ROLE_MAPPING` DISABLE KEYS */;
/*!40000 ALTER TABLE `GROUP_ROLE_MAPPING` ENABLE KEYS */;

--
-- Table structure for table `IDENTITY_PROVIDER`
--

DROP TABLE IF EXISTS `IDENTITY_PROVIDER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `IDENTITY_PROVIDER` (
  `INTERNAL_ID` varchar(36) NOT NULL,
  `ENABLED` tinyint NOT NULL DEFAULT '0',
  `PROVIDER_ALIAS` varchar(255) DEFAULT NULL,
  `PROVIDER_ID` varchar(255) DEFAULT NULL,
  `STORE_TOKEN` tinyint NOT NULL DEFAULT '0',
  `AUTHENTICATE_BY_DEFAULT` tinyint NOT NULL DEFAULT '0',
  `REALM_ID` varchar(36) DEFAULT NULL,
  `ADD_TOKEN_ROLE` tinyint NOT NULL DEFAULT '1',
  `TRUST_EMAIL` tinyint NOT NULL DEFAULT '0',
  `FIRST_BROKER_LOGIN_FLOW_ID` varchar(36) DEFAULT NULL,
  `POST_BROKER_LOGIN_FLOW_ID` varchar(36) DEFAULT NULL,
  `PROVIDER_DISPLAY_NAME` varchar(255) DEFAULT NULL,
  `LINK_ONLY` tinyint NOT NULL DEFAULT '0',
  `ORGANIZATION_ID` varchar(255) DEFAULT NULL,
  `HIDE_ON_LOGIN` tinyint DEFAULT '0',
  PRIMARY KEY (`INTERNAL_ID`),
  UNIQUE KEY `UK_2DAELWNIBJI49AVXSRTUF6XJ33` (`PROVIDER_ALIAS`,`REALM_ID`),
  KEY `IDX_IDENT_PROV_REALM` (`REALM_ID`),
  KEY `IDX_IDP_REALM_ORG` (`REALM_ID`,`ORGANIZATION_ID`),
  KEY `IDX_IDP_FOR_LOGIN` (`REALM_ID`,`ENABLED`,`LINK_ONLY`,`HIDE_ON_LOGIN`,`ORGANIZATION_ID`),
  CONSTRAINT `FK2B4EBC52AE5C3B34` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `IDENTITY_PROVIDER`
--

/*!40000 ALTER TABLE `IDENTITY_PROVIDER` DISABLE KEYS */;
INSERT INTO `IDENTITY_PROVIDER` VALUES ('13aac26d-98eb-4c39-9a66-313668b73bed',1,'google','google',0,0,'b90e8c11-9258-44ee-901a-1650b6e14601',0,0,NULL,NULL,'Google login',0,NULL,0);
/*!40000 ALTER TABLE `IDENTITY_PROVIDER` ENABLE KEYS */;

--
-- Table structure for table `IDENTITY_PROVIDER_CONFIG`
--

DROP TABLE IF EXISTS `IDENTITY_PROVIDER_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `IDENTITY_PROVIDER_CONFIG` (
  `IDENTITY_PROVIDER_ID` varchar(36) NOT NULL,
  `VALUE` longtext,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`IDENTITY_PROVIDER_ID`,`NAME`),
  CONSTRAINT `FKDC4897CF864C4E43` FOREIGN KEY (`IDENTITY_PROVIDER_ID`) REFERENCES `IDENTITY_PROVIDER` (`INTERNAL_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `IDENTITY_PROVIDER_CONFIG`
--

/*!40000 ALTER TABLE `IDENTITY_PROVIDER_CONFIG` DISABLE KEYS */;
INSERT INTO `IDENTITY_PROVIDER_CONFIG` VALUES ('13aac26d-98eb-4c39-9a66-313668b73bed','doantotnghiep','clientId'),('13aac26d-98eb-4c39-9a66-313668b73bed','46sGOyba6nyt8UhLkKAQgzmbedF9L042','clientSecret'),('13aac26d-98eb-4c39-9a66-313668b73bed','true','offlineAccess');
/*!40000 ALTER TABLE `IDENTITY_PROVIDER_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `IDENTITY_PROVIDER_MAPPER`
--

DROP TABLE IF EXISTS `IDENTITY_PROVIDER_MAPPER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `IDENTITY_PROVIDER_MAPPER` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `IDP_ALIAS` varchar(255) NOT NULL,
  `IDP_MAPPER_NAME` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_ID_PROV_MAPP_REALM` (`REALM_ID`),
  CONSTRAINT `FK_IDPM_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `IDENTITY_PROVIDER_MAPPER`
--

/*!40000 ALTER TABLE `IDENTITY_PROVIDER_MAPPER` DISABLE KEYS */;
INSERT INTO `IDENTITY_PROVIDER_MAPPER` VALUES ('302d81e4-37e6-49c4-b890-25aa9a28a46d','Email with google','google','google-user-attribute-mapper','b90e8c11-9258-44ee-901a-1650b6e14601');
/*!40000 ALTER TABLE `IDENTITY_PROVIDER_MAPPER` ENABLE KEYS */;

--
-- Table structure for table `IDP_MAPPER_CONFIG`
--

DROP TABLE IF EXISTS `IDP_MAPPER_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `IDP_MAPPER_CONFIG` (
  `IDP_MAPPER_ID` varchar(36) NOT NULL,
  `VALUE` longtext,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`IDP_MAPPER_ID`,`NAME`),
  CONSTRAINT `FK_IDPMCONFIG` FOREIGN KEY (`IDP_MAPPER_ID`) REFERENCES `IDENTITY_PROVIDER_MAPPER` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `IDP_MAPPER_CONFIG`
--

/*!40000 ALTER TABLE `IDP_MAPPER_CONFIG` DISABLE KEYS */;
INSERT INTO `IDP_MAPPER_CONFIG` VALUES ('302d81e4-37e6-49c4-b890-25aa9a28a46d','INHERIT','syncMode'),('302d81e4-37e6-49c4-b890-25aa9a28a46d','email','userAttribute');
/*!40000 ALTER TABLE `IDP_MAPPER_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `JGROUPS_PING`
--

DROP TABLE IF EXISTS `JGROUPS_PING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `JGROUPS_PING` (
  `address` varchar(200) NOT NULL,
  `name` varchar(200) DEFAULT NULL,
  `cluster_name` varchar(200) NOT NULL,
  `ip` varchar(200) NOT NULL,
  `coord` tinyint DEFAULT NULL,
  PRIMARY KEY (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `JGROUPS_PING`
--

/*!40000 ALTER TABLE `JGROUPS_PING` DISABLE KEYS */;
/*!40000 ALTER TABLE `JGROUPS_PING` ENABLE KEYS */;

--
-- Table structure for table `KEYCLOAK_GROUP`
--

DROP TABLE IF EXISTS `KEYCLOAK_GROUP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `KEYCLOAK_GROUP` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `PARENT_GROUP` varchar(36) NOT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  `TYPE` int NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `SIBLING_NAMES` (`REALM_ID`,`PARENT_GROUP`,`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KEYCLOAK_GROUP`
--

/*!40000 ALTER TABLE `KEYCLOAK_GROUP` DISABLE KEYS */;
/*!40000 ALTER TABLE `KEYCLOAK_GROUP` ENABLE KEYS */;

--
-- Table structure for table `KEYCLOAK_ROLE`
--

DROP TABLE IF EXISTS `KEYCLOAK_ROLE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `KEYCLOAK_ROLE` (
  `ID` varchar(36) NOT NULL,
  `CLIENT_REALM_CONSTRAINT` varchar(255) DEFAULT NULL,
  `CLIENT_ROLE` tinyint DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `NAME` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `REALM_ID` varchar(255) DEFAULT NULL,
  `CLIENT` varchar(36) DEFAULT NULL,
  `REALM` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_J3RWUVD56ONTGSUHOGM184WW2-2` (`NAME`,`CLIENT_REALM_CONSTRAINT`),
  KEY `IDX_KEYCLOAK_ROLE_CLIENT` (`CLIENT`),
  KEY `IDX_KEYCLOAK_ROLE_REALM` (`REALM`),
  CONSTRAINT `FK_6VYQFE4CN4WLQ8R6KT5VDSJ5C` FOREIGN KEY (`REALM`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KEYCLOAK_ROLE`
--

/*!40000 ALTER TABLE `KEYCLOAK_ROLE` DISABLE KEYS */;
INSERT INTO `KEYCLOAK_ROLE` VALUES ('02112d05-ab93-428e-9487-845c27a20b1f','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_query-users}','query-users','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('02c57b11-11ba-4679-b9a9-a3ea45b8f157','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_query-groups}','query-groups','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('0393b358-a958-4f83-bc13-e33b7e27a5ee','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_view-events}','view-events','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('06bd840d-d8b2-4262-92e9-efc268fc5bd4','736d3e5b-4c46-41ed-9afb-47e727ecab9e',0,'${role_offline-access}','offline_access','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL,NULL),('071a31b5-330c-4931-b8e6-348573d4774a','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_manage-users}','manage-users','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('08a0b56f-7647-4340-9622-15a85b1d33d8','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_query-groups}','query-groups','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('13a15d8f-e685-4321-a8e9-9b4299dfef76','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_manage-account}','manage-account','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('17a79d1e-453a-4599-bc0d-09385ba9c6ca','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_view-realm}','view-realm','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('189dc51b-311a-4786-b014-74ab2bff6673','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_view-identity-providers}','view-identity-providers','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('1c56677f-1a5f-47e7-ac29-cd386bf7765d','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_query-realms}','query-realms','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('25180c3d-0961-4107-9462-d73128d2443f','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_view-realm}','view-realm','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('25b888c8-7935-430b-8927-9d244a39060b','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_manage-account}','manage-account','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('25e8a5c0-77d3-4ea0-a80e-2bc0b731bb92','ba733c9c-d2a2-4464-a77d-d6b393a36968',1,'${role_read-token}','read-token','b90e8c11-9258-44ee-901a-1650b6e14601','ba733c9c-d2a2-4464-a77d-d6b393a36968',NULL),('28387ce2-eece-409c-ac39-169371204360','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_delete-account}','delete-account','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('368e8d6a-5b1d-46b0-9b37-2584c3accb8c','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_view-clients}','view-clients','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('3a2f4883-a668-4a66-b8c0-a774c0facd86','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_manage-realm}','manage-realm','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('3e038d42-14ca-48ee-a4bd-cec4ec558287','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_manage-clients}','manage-clients','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('42fc327d-f710-4e25-889b-7ebfdb1d9daa','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_query-users}','query-users','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('45f31d92-6f8a-43d3-82a4-e14da34873b3','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_manage-users}','manage-users','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('490aa571-2591-43df-b357-7f1bd4bd0c54','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_query-realms}','query-realms','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('4de0e63f-abd5-427c-aab2-2ed7f8a58ec0','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_manage-events}','manage-events','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('4eb3aa14-3436-4180-8909-aacbdf43659e','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_manage-clients}','manage-clients','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('50cddbcb-58b8-4948-be8f-cb4ab7979bcb','736d3e5b-4c46-41ed-9afb-47e727ecab9e',0,'${role_admin}','admin','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL,NULL),('5424451b-8a17-4a8d-a590-9b4b7ef59010','b90e8c11-9258-44ee-901a-1650b6e14601',0,'${role_uma_authorization}','uma_authorization','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,NULL),('57efb658-e3b8-409b-8ae9-d406bb04335b','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_manage-realm}','manage-realm','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('59b77fbd-731f-402e-adca-ae5c779e5505','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_impersonation}','impersonation','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('5bb9f798-176d-4de0-a595-06e7067df4d2','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_manage-identity-providers}','manage-identity-providers','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('60dbc25e-b0fa-4335-8251-cef1e45d2465','b90e8c11-9258-44ee-901a-1650b6e14601',0,'${role_default-roles}','default-roles-keycloak','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,NULL),('67118b40-69ce-4576-95e2-c215710a7e8c','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_query-clients}','query-clients','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('69146af4-f2ee-40d3-a5f3-bf885dc447cc','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_query-realms}','query-realms','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('696c50b7-1ca4-487a-be10-35af1b1edf30','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_manage-account-links}','manage-account-links','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('69c0ec5c-4099-4e2d-87c9-637d35ea738e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_view-events}','view-events','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('6a28c7be-f203-4d41-8a3a-e4d9b89bd928','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_query-clients}','query-clients','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('6b1f87ee-2c31-4de2-967e-c802c8c24b27','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_create-client}','create-client','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('6bacd7b8-5e28-4ded-9332-cfa9398fd738','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_manage-realm}','manage-realm','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('6be05d3b-a559-498f-bb01-daf2ca0eb51e','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_view-clients}','view-clients','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('6ca12eef-2b56-4303-86d8-7d90befe9e87','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_manage-account-links}','manage-account-links','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('6d242725-cf40-47db-a7a9-4be50a7d595b','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_manage-users}','manage-users','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('75696880-7f61-45d5-b326-a5e8b021bb9f','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_query-users}','query-users','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('7a25ff1e-92a6-4a52-9b10-1ae0b1284305','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_create-client}','create-client','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('7cd24a2d-2b98-41da-9d40-64e04b44a0f0','028d7f5b-9f03-4977-bc22-d7ddad3abbdf',1,'','staff','b90e8c11-9258-44ee-901a-1650b6e14601','028d7f5b-9f03-4977-bc22-d7ddad3abbdf',NULL),('7e1512be-5397-406b-9181-86bd319a6493','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_manage-consent}','manage-consent','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('7ff5492c-9bc7-43d8-87c4-b719d65547e9','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_create-client}','create-client','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('80aae6a3-6bcb-4f11-80e7-1921e2035cd8','028d7f5b-9f03-4977-bc22-d7ddad3abbdf',1,'','client','b90e8c11-9258-44ee-901a-1650b6e14601','028d7f5b-9f03-4977-bc22-d7ddad3abbdf',NULL),('82bab180-d568-4a04-a1f4-cfb93bb94599','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_view-authorization}','view-authorization','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('838a91b6-a65d-4539-9d78-30a598916488','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_impersonation}','impersonation','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('89603119-2285-420c-beac-aec506afd06a','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_view-groups}','view-groups','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('89b819f5-80eb-491c-a888-63e8dbec7c7d','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_manage-identity-providers}','manage-identity-providers','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('8a7a293c-d5a8-4a09-8620-9eb77007816a','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_view-applications}','view-applications','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('8b453d03-a970-4732-b665-70e6266aa38e','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_query-groups}','query-groups','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('8b555b9e-3761-4404-85f4-dfa581bdb5cf','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_view-applications}','view-applications','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('8c5ddd89-6325-4a03-b1e0-f4e8e6073507','736d3e5b-4c46-41ed-9afb-47e727ecab9e',0,'${role_uma_authorization}','uma_authorization','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL,NULL),('8eca61e5-db73-499c-869d-063c7f52f4cc','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_view-profile}','view-profile','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('8f69e23d-14c1-4363-a38c-07326bac027e','736d3e5b-4c46-41ed-9afb-47e727ecab9e',0,'${role_create-realm}','create-realm','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL,NULL),('9771e873-0be5-4096-aab2-6a689d4b1796','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_view-authorization}','view-authorization','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('9964030e-8d82-4291-9dee-a23718b69590','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_view-events}','view-events','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('9d2a9df4-8da5-48c3-b575-93347476f3d6','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_manage-authorization}','manage-authorization','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('a0ffeee3-63ba-46fe-9c67-0d11938ba6e4','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_manage-consent}','manage-consent','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('a2336e2e-843e-47a5-a16a-2f10216f7d44','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_manage-clients}','manage-clients','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('a58950e1-dd22-42b0-952f-b2b348d48a36','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_manage-authorization}','manage-authorization','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('a5b6690f-db3f-4772-ac95-07865102c3cb','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_view-realm}','view-realm','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('acb63854-a657-42c4-a70d-cbd710bff9e0','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_view-users}','view-users','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('ad629a28-6aa3-49a9-a10b-5cb07f3c4de3','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_view-authorization}','view-authorization','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('ae248f1c-1714-431b-a1ce-3b288937f60a','540a1782-e266-4d93-8898-fd85e3ca4825',1,'${role_read-token}','read-token','736d3e5b-4c46-41ed-9afb-47e727ecab9e','540a1782-e266-4d93-8898-fd85e3ca4825',NULL),('b8e5dbef-a678-4071-872d-f019ec318cc6','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_delete-account}','delete-account','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('bac7bce2-1afa-4164-a4e1-54d36c788834','b90e8c11-9258-44ee-901a-1650b6e14601',0,'${role_offline-access}','offline_access','b90e8c11-9258-44ee-901a-1650b6e14601',NULL,NULL),('bf1f74b6-8c60-4b33-a88d-e77620bae619','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_view-clients}','view-clients','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('bf764633-fad6-4f0b-9ded-68d89eadea28','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_view-consent}','view-consent','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('c5f9f487-cf44-4c73-9b16-0fde2924bd00','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_view-users}','view-users','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('c70f977c-1f7d-45d3-a4b1-fecc8757df50','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_view-identity-providers}','view-identity-providers','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('c8004809-83f1-4023-9a0f-0e55e797a40e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_view-groups}','view-groups','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('c83a4447-52e6-4451-9737-38b9619c828d','70ac515d-37ca-4129-9a1f-9aceab32bcd0',1,'${role_view-consent}','view-consent','736d3e5b-4c46-41ed-9afb-47e727ecab9e','70ac515d-37ca-4129-9a1f-9aceab32bcd0',NULL),('d35f090e-11ff-4e3f-ba25-7be1fd082b4c','36d6476a-889e-4395-bf12-9fc79c217c36',1,'${role_manage-events}','manage-events','736d3e5b-4c46-41ed-9afb-47e727ecab9e','36d6476a-889e-4395-bf12-9fc79c217c36',NULL),('d5d76baa-b456-4888-aecf-e359a4704f78','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',1,'${role_view-profile}','view-profile','b90e8c11-9258-44ee-901a-1650b6e14601','5899f04c-e3bf-467b-bf8d-ad2b5bc031fa',NULL),('d9245161-e9f0-4479-86e5-92d9c6ef3c83','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_impersonation}','impersonation','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('db9f0d3b-d226-4d74-a911-6b21e171c1d3','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_manage-events}','manage-events','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('de10e7a6-8542-4240-91a9-b7323ba3c2c7','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_manage-authorization}','manage-authorization','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('e5ed0422-2d74-421a-96a6-e667f85a0ebd','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_manage-identity-providers}','manage-identity-providers','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('e61e14ab-a1b6-47e3-a182-0c80ce0cc23b','736d3e5b-4c46-41ed-9afb-47e727ecab9e',0,'${role_default-roles}','default-roles-master','736d3e5b-4c46-41ed-9afb-47e727ecab9e',NULL,NULL),('e8f934e2-5ff2-414b-9ea7-7e2d53556e32','3fc95085-aba5-4a9e-95d5-532e3afad9f2',1,'${role_view-identity-providers}','view-identity-providers','736d3e5b-4c46-41ed-9afb-47e727ecab9e','3fc95085-aba5-4a9e-95d5-532e3afad9f2',NULL),('efe37c6e-668b-42b6-a496-185f95031b0f','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_view-users}','view-users','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('f2b04fa6-137f-4201-88d6-18bc49b7e83e','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_realm-admin}','realm-admin','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('f3882302-e087-474c-b21e-1efc409bc5e8','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',1,'${role_query-clients}','query-clients','b90e8c11-9258-44ee-901a-1650b6e14601','1aa38202-97c8-4c48-9f97-d1b65bdb84c8',NULL),('f6b36520-6058-4b52-8e0b-d7230c21c53f','028d7f5b-9f03-4977-bc22-d7ddad3abbdf',1,'','admin','b90e8c11-9258-44ee-901a-1650b6e14601','028d7f5b-9f03-4977-bc22-d7ddad3abbdf',NULL);
/*!40000 ALTER TABLE `KEYCLOAK_ROLE` ENABLE KEYS */;

--
-- Table structure for table `MIGRATION_MODEL`
--

DROP TABLE IF EXISTS `MIGRATION_MODEL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MIGRATION_MODEL` (
  `ID` varchar(36) NOT NULL,
  `VERSION` varchar(36) DEFAULT NULL,
  `UPDATE_TIME` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `IDX_UPDATE_TIME` (`UPDATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MIGRATION_MODEL`
--

/*!40000 ALTER TABLE `MIGRATION_MODEL` DISABLE KEYS */;
INSERT INTO `MIGRATION_MODEL` VALUES ('emz4y','26.2.5',1751810242);
/*!40000 ALTER TABLE `MIGRATION_MODEL` ENABLE KEYS */;

--
-- Table structure for table `OFFLINE_CLIENT_SESSION`
--

DROP TABLE IF EXISTS `OFFLINE_CLIENT_SESSION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `OFFLINE_CLIENT_SESSION` (
  `USER_SESSION_ID` varchar(36) NOT NULL,
  `CLIENT_ID` varchar(255) NOT NULL,
  `OFFLINE_FLAG` varchar(4) NOT NULL,
  `TIMESTAMP` int DEFAULT NULL,
  `DATA` longtext,
  `CLIENT_STORAGE_PROVIDER` varchar(36) NOT NULL DEFAULT 'local',
  `EXTERNAL_CLIENT_ID` varchar(255) NOT NULL DEFAULT 'local',
  `VERSION` int DEFAULT '0',
  PRIMARY KEY (`USER_SESSION_ID`,`CLIENT_ID`,`CLIENT_STORAGE_PROVIDER`,`EXTERNAL_CLIENT_ID`,`OFFLINE_FLAG`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `OFFLINE_CLIENT_SESSION`
--

/*!40000 ALTER TABLE `OFFLINE_CLIENT_SESSION` DISABLE KEYS */;
INSERT INTO `OFFLINE_CLIENT_SESSION` VALUES ('2ac2b761-47d2-4483-9d29-801e060e0555','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','0',1756143788,'{\"authMethod\":\"openid-connect\",\"notes\":{\"clientId\":\"028d7f5b-9f03-4977-bc22-d7ddad3abbdf\",\"userSessionStartedAt\":\"1756143788\",\"iss\":\"http://keycloak:8080/realms/keycloak\",\"startedAt\":\"1756143788\",\"level-of-authentication\":\"-1\"}}','local','local',0),('70ab6c6e-2f8b-4afe-b86f-57fea78f0e42','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','0',1756143798,'{\"authMethod\":\"openid-connect\",\"notes\":{\"clientId\":\"028d7f5b-9f03-4977-bc22-d7ddad3abbdf\",\"userSessionStartedAt\":\"1756143798\",\"iss\":\"http://keycloak:8080/realms/keycloak\",\"startedAt\":\"1756143798\",\"level-of-authentication\":\"-1\"}}','local','local',0),('955c0541-4bfd-4ff5-84bd-64e873d48aa2','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','0',1756143803,'{\"authMethod\":\"openid-connect\",\"notes\":{\"clientId\":\"028d7f5b-9f03-4977-bc22-d7ddad3abbdf\",\"userSessionStartedAt\":\"1756143803\",\"iss\":\"http://keycloak:8080/realms/keycloak\",\"startedAt\":\"1756143803\",\"level-of-authentication\":\"-1\"}}','local','local',0),('a2ed0763-002b-4d60-bb3b-e5cae95fb930','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','0',1756143816,'{\"authMethod\":\"openid-connect\",\"notes\":{\"clientId\":\"028d7f5b-9f03-4977-bc22-d7ddad3abbdf\",\"userSessionStartedAt\":\"1756143816\",\"iss\":\"http://keycloak:8080/realms/keycloak\",\"startedAt\":\"1756143816\",\"level-of-authentication\":\"-1\"}}','local','local',0),('cde2efec-0eeb-4723-acde-c9cc5d0aee37','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','0',1756143814,'{\"authMethod\":\"openid-connect\",\"notes\":{\"clientId\":\"028d7f5b-9f03-4977-bc22-d7ddad3abbdf\",\"userSessionStartedAt\":\"1756143814\",\"iss\":\"http://keycloak:8080/realms/keycloak\",\"startedAt\":\"1756143814\",\"level-of-authentication\":\"-1\"}}','local','local',0),('d5103301-3169-4c4d-bfc0-9d55672fdbd1','028d7f5b-9f03-4977-bc22-d7ddad3abbdf','0',1756143810,'{\"authMethod\":\"openid-connect\",\"notes\":{\"clientId\":\"028d7f5b-9f03-4977-bc22-d7ddad3abbdf\",\"userSessionStartedAt\":\"1756143810\",\"iss\":\"http://keycloak:8080/realms/keycloak\",\"startedAt\":\"1756143810\",\"level-of-authentication\":\"-1\"}}','local','local',0);
/*!40000 ALTER TABLE `OFFLINE_CLIENT_SESSION` ENABLE KEYS */;

--
-- Table structure for table `OFFLINE_USER_SESSION`
--

DROP TABLE IF EXISTS `OFFLINE_USER_SESSION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `OFFLINE_USER_SESSION` (
  `USER_SESSION_ID` varchar(36) NOT NULL,
  `USER_ID` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `CREATED_ON` int NOT NULL,
  `OFFLINE_FLAG` varchar(4) NOT NULL,
  `DATA` longtext,
  `LAST_SESSION_REFRESH` int NOT NULL DEFAULT '0',
  `BROKER_SESSION_ID` text,
  `VERSION` int DEFAULT '0',
  PRIMARY KEY (`USER_SESSION_ID`,`OFFLINE_FLAG`),
  KEY `IDX_OFFLINE_USS_BY_USER` (`USER_ID`,`REALM_ID`,`OFFLINE_FLAG`),
  KEY `IDX_OFFLINE_USS_BY_LAST_SESSION_REFRESH` (`REALM_ID`,`OFFLINE_FLAG`,`LAST_SESSION_REFRESH`),
  KEY `IDX_OFFLINE_USS_BY_BROKER_SESSION_ID` (`BROKER_SESSION_ID`(255),`REALM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `OFFLINE_USER_SESSION`
--

/*!40000 ALTER TABLE `OFFLINE_USER_SESSION` DISABLE KEYS */;
INSERT INTO `OFFLINE_USER_SESSION` VALUES ('2ac2b761-47d2-4483-9d29-801e060e0555','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','b90e8c11-9258-44ee-901a-1650b6e14601',1756143788,'0','{\"ipAddress\":\"172.18.0.4\",\"authMethod\":\"openid-connect\",\"rememberMe\":false,\"started\":0,\"notes\":{\"KC_DEVICE_NOTE\":\"eyJpcEFkZHJlc3MiOiIxNzIuMTguMC40Iiwib3MiOiJPdGhlciIsIm9zVmVyc2lvbiI6IlVua25vd24iLCJicm93c2VyIjoiQXBhY2hlLUh0dHBDbGllbnQvNC41LjE0IiwiZGV2aWNlIjoiT3RoZXIiLCJsYXN0QWNjZXNzIjowLCJtb2JpbGUiOmZhbHNlfQ==\",\"authenticators-completed\":\"{\\\"c4fada70-fee2-492f-a2a9-b7b69b1a2b6a\\\":1756143785,\\\"07c6a381-0bfa-4db3-94f4-5a59c60266d4\\\":1756143787}\"},\"state\":\"LOGGED_IN\"}',1756143788,NULL,0),('70ab6c6e-2f8b-4afe-b86f-57fea78f0e42','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','b90e8c11-9258-44ee-901a-1650b6e14601',1756143798,'0','{\"ipAddress\":\"172.18.0.4\",\"authMethod\":\"openid-connect\",\"rememberMe\":false,\"started\":0,\"notes\":{\"KC_DEVICE_NOTE\":\"eyJpcEFkZHJlc3MiOiIxNzIuMTguMC40Iiwib3MiOiJPdGhlciIsIm9zVmVyc2lvbiI6IlVua25vd24iLCJicm93c2VyIjoiQXBhY2hlLUh0dHBDbGllbnQvNC41LjE0IiwiZGV2aWNlIjoiT3RoZXIiLCJsYXN0QWNjZXNzIjowLCJtb2JpbGUiOmZhbHNlfQ==\",\"authenticators-completed\":\"{\\\"c4fada70-fee2-492f-a2a9-b7b69b1a2b6a\\\":1756143797,\\\"07c6a381-0bfa-4db3-94f4-5a59c60266d4\\\":1756143798}\"},\"state\":\"LOGGED_IN\"}',1756143798,NULL,0),('955c0541-4bfd-4ff5-84bd-64e873d48aa2','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','b90e8c11-9258-44ee-901a-1650b6e14601',1756143803,'0','{\"ipAddress\":\"172.18.0.4\",\"authMethod\":\"openid-connect\",\"rememberMe\":false,\"started\":0,\"notes\":{\"KC_DEVICE_NOTE\":\"eyJpcEFkZHJlc3MiOiIxNzIuMTguMC40Iiwib3MiOiJPdGhlciIsIm9zVmVyc2lvbiI6IlVua25vd24iLCJicm93c2VyIjoiQXBhY2hlLUh0dHBDbGllbnQvNC41LjE0IiwiZGV2aWNlIjoiT3RoZXIiLCJsYXN0QWNjZXNzIjowLCJtb2JpbGUiOmZhbHNlfQ==\",\"authenticators-completed\":\"{\\\"c4fada70-fee2-492f-a2a9-b7b69b1a2b6a\\\":1756143802,\\\"07c6a381-0bfa-4db3-94f4-5a59c60266d4\\\":1756143803}\"},\"state\":\"LOGGED_IN\"}',1756143803,NULL,0),('a2ed0763-002b-4d60-bb3b-e5cae95fb930','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','b90e8c11-9258-44ee-901a-1650b6e14601',1756143816,'0','{\"ipAddress\":\"172.18.0.4\",\"authMethod\":\"openid-connect\",\"rememberMe\":false,\"started\":0,\"notes\":{\"KC_DEVICE_NOTE\":\"eyJpcEFkZHJlc3MiOiIxNzIuMTguMC40Iiwib3MiOiJPdGhlciIsIm9zVmVyc2lvbiI6IlVua25vd24iLCJicm93c2VyIjoiQXBhY2hlLUh0dHBDbGllbnQvNC41LjE0IiwiZGV2aWNlIjoiT3RoZXIiLCJsYXN0QWNjZXNzIjowLCJtb2JpbGUiOmZhbHNlfQ==\",\"authenticators-completed\":\"{\\\"c4fada70-fee2-492f-a2a9-b7b69b1a2b6a\\\":1756143816,\\\"07c6a381-0bfa-4db3-94f4-5a59c60266d4\\\":1756143816}\"},\"state\":\"LOGGED_IN\"}',1756143816,NULL,0),('cde2efec-0eeb-4723-acde-c9cc5d0aee37','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','b90e8c11-9258-44ee-901a-1650b6e14601',1756143814,'0','{\"ipAddress\":\"172.18.0.4\",\"authMethod\":\"openid-connect\",\"rememberMe\":false,\"started\":0,\"notes\":{\"KC_DEVICE_NOTE\":\"eyJpcEFkZHJlc3MiOiIxNzIuMTguMC40Iiwib3MiOiJPdGhlciIsIm9zVmVyc2lvbiI6IlVua25vd24iLCJicm93c2VyIjoiQXBhY2hlLUh0dHBDbGllbnQvNC41LjE0IiwiZGV2aWNlIjoiT3RoZXIiLCJsYXN0QWNjZXNzIjowLCJtb2JpbGUiOmZhbHNlfQ==\",\"authenticators-completed\":\"{\\\"c4fada70-fee2-492f-a2a9-b7b69b1a2b6a\\\":1756143814,\\\"07c6a381-0bfa-4db3-94f4-5a59c60266d4\\\":1756143814}\"},\"state\":\"LOGGED_IN\"}',1756143814,NULL,0),('d5103301-3169-4c4d-bfc0-9d55672fdbd1','4030193c-c653-4e74-a4b4-6fd4e06fb4cf','b90e8c11-9258-44ee-901a-1650b6e14601',1756143810,'0','{\"ipAddress\":\"172.18.0.4\",\"authMethod\":\"openid-connect\",\"rememberMe\":false,\"started\":0,\"notes\":{\"KC_DEVICE_NOTE\":\"eyJpcEFkZHJlc3MiOiIxNzIuMTguMC40Iiwib3MiOiJPdGhlciIsIm9zVmVyc2lvbiI6IlVua25vd24iLCJicm93c2VyIjoiQXBhY2hlLUh0dHBDbGllbnQvNC41LjE0IiwiZGV2aWNlIjoiT3RoZXIiLCJsYXN0QWNjZXNzIjowLCJtb2JpbGUiOmZhbHNlfQ==\",\"authenticators-completed\":\"{\\\"c4fada70-fee2-492f-a2a9-b7b69b1a2b6a\\\":1756143810,\\\"07c6a381-0bfa-4db3-94f4-5a59c60266d4\\\":1756143810}\"},\"state\":\"LOGGED_IN\"}',1756143810,NULL,0);
/*!40000 ALTER TABLE `OFFLINE_USER_SESSION` ENABLE KEYS */;

--
-- Table structure for table `ORG`
--

DROP TABLE IF EXISTS `ORG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ORG` (
  `ID` varchar(255) NOT NULL,
  `ENABLED` tinyint NOT NULL,
  `REALM_ID` varchar(255) NOT NULL,
  `GROUP_ID` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `DESCRIPTION` text,
  `ALIAS` varchar(255) NOT NULL,
  `REDIRECT_URL` text,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_ORG_NAME` (`REALM_ID`,`NAME`),
  UNIQUE KEY `UK_ORG_GROUP` (`GROUP_ID`),
  UNIQUE KEY `UK_ORG_ALIAS` (`REALM_ID`,`ALIAS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ORG`
--

/*!40000 ALTER TABLE `ORG` DISABLE KEYS */;
/*!40000 ALTER TABLE `ORG` ENABLE KEYS */;

--
-- Table structure for table `ORG_DOMAIN`
--

DROP TABLE IF EXISTS `ORG_DOMAIN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ORG_DOMAIN` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VERIFIED` tinyint NOT NULL,
  `ORG_ID` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`,`NAME`),
  KEY `IDX_ORG_DOMAIN_ORG_ID` (`ORG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ORG_DOMAIN`
--

/*!40000 ALTER TABLE `ORG_DOMAIN` DISABLE KEYS */;
/*!40000 ALTER TABLE `ORG_DOMAIN` ENABLE KEYS */;

--
-- Table structure for table `POLICY_CONFIG`
--

DROP TABLE IF EXISTS `POLICY_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `POLICY_CONFIG` (
  `POLICY_ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` longtext,
  PRIMARY KEY (`POLICY_ID`,`NAME`),
  CONSTRAINT `FKDC34197CF864C4E43` FOREIGN KEY (`POLICY_ID`) REFERENCES `RESOURCE_SERVER_POLICY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `POLICY_CONFIG`
--

/*!40000 ALTER TABLE `POLICY_CONFIG` DISABLE KEYS */;
/*!40000 ALTER TABLE `POLICY_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `PROTOCOL_MAPPER`
--

DROP TABLE IF EXISTS `PROTOCOL_MAPPER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PROTOCOL_MAPPER` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `PROTOCOL` varchar(255) NOT NULL,
  `PROTOCOL_MAPPER_NAME` varchar(255) NOT NULL,
  `CLIENT_ID` varchar(36) DEFAULT NULL,
  `CLIENT_SCOPE_ID` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_PROTOCOL_MAPPER_CLIENT` (`CLIENT_ID`),
  KEY `IDX_CLSCOPE_PROTMAP` (`CLIENT_SCOPE_ID`),
  CONSTRAINT `FK_CLI_SCOPE_MAPPER` FOREIGN KEY (`CLIENT_SCOPE_ID`) REFERENCES `CLIENT_SCOPE` (`ID`),
  CONSTRAINT `FK_PCM_REALM` FOREIGN KEY (`CLIENT_ID`) REFERENCES `CLIENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PROTOCOL_MAPPER`
--

/*!40000 ALTER TABLE `PROTOCOL_MAPPER` DISABLE KEYS */;
INSERT INTO `PROTOCOL_MAPPER` VALUES ('055a5c5f-6641-4cc3-90e4-238053851a49','allowed web origins','openid-connect','oidc-allowed-origins-mapper',NULL,'33dba1c8-8c1c-4128-b02f-4f10b90f97a2'),('092c4112-ab38-4c4a-b00e-4737c959337a','phone number verified','openid-connect','oidc-usermodel-attribute-mapper',NULL,'5619f20a-e951-410e-af88-38084f0f498b'),('0d3986d9-9945-42ea-9f36-4c4b2c14c637','audience resolve','openid-connect','oidc-audience-resolve-mapper',NULL,'44bc3d1e-d675-4a76-b1a8-c976fbd843cd'),('10c60ef0-1678-43bb-8dce-71940370a216','family name','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('11c146da-9791-4b43-a132-d3eea8ddb12e','middle name','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('1da16af7-a9dc-454e-8c79-517bf9df59e6','sub','openid-connect','oidc-sub-mapper',NULL,'e61927e1-ec5d-4137-8617-31b436b5c9be'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','picture','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','updated at','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','birthdate','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','upn','openid-connect','oidc-usermodel-attribute-mapper',NULL,'f13e4778-609b-484d-affb-3ba1856b32e0'),('25f5665a-e12a-493d-8541-593bd82ba862','given name','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('2caeccff-aa98-4236-9729-3c8923856a23','acr loa level','openid-connect','oidc-acr-mapper',NULL,'aaa4253c-d77a-4b6e-a67a-2dab2ee49116'),('3cff59ce-fe4a-4220-a8c1-904b44b4772f','organization','saml','saml-organization-membership-mapper',NULL,'f420fc2a-382f-4dd0-8d2e-ac376bd22c32'),('3d0c686e-9d3c-4ae4-bf2c-74cec9cf4f3e','organization','openid-connect','oidc-organization-membership-mapper',NULL,'324d157a-d679-4372-adbd-a8f13aa424af'),('400b3bdb-7310-4c6b-b74f-a53915bde681','Client IP Address','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'e94865e0-603b-4582-b7d4-90151939f139'),('411506ad-6196-430c-8b42-1ba0723a03b8','groups','openid-connect','oidc-usermodel-realm-role-mapper',NULL,'f13e4778-609b-484d-affb-3ba1856b32e0'),('4251d783-fd2a-4fcd-9dda-b0bdaced2507','allowed web origins','openid-connect','oidc-allowed-origins-mapper',NULL,'b503aa77-481f-4457-ad9b-1c444f7d82bd'),('44dc18c4-a197-4494-b4d8-0672458f69c4','website','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','phone number','openid-connect','oidc-usermodel-attribute-mapper',NULL,'453424f4-1a6e-412f-ace0-7c92331cc1e4'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','birthdate','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('4bb6c4fe-8b04-4bd8-94c9-969edd50749a','auth_time','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'a1327838-8323-46e5-b4c2-85da961df597'),('505a236a-0dbb-4eec-a431-006bc72ce764','zoneinfo','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('5063bb39-a664-48ac-864d-48779221b653','username','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','given name','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('593da648-193d-447c-9ba3-94a07fcf6c55','role list','saml','saml-role-list-mapper',NULL,'ceb884c9-b61b-4b39-af8f-e5c63106791b'),('596cb309-0292-4989-b434-9edf648f3efc','groups','openid-connect','oidc-usermodel-realm-role-mapper',NULL,'47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f'),('689ec476-fded-4c71-b583-368473b661be','acr loa level','openid-connect','oidc-acr-mapper',NULL,'c5ba5bb8-8431-4542-acd4-f9de2730c7d1'),('693040b1-c49e-4ae7-baaa-777a554e635b','email','openid-connect','oidc-usermodel-attribute-mapper',NULL,'d59644b7-45b9-4c40-9004-ad0f4c922689'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','website','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','middle name','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('70dcce46-d2de-41c1-afea-1ebe242a5507','nickname','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('754bb346-6796-4460-8ad6-5aac0c34275b','zoneinfo','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('77ed86f8-cf6e-47ed-89f1-e87161c9180c','full name','openid-connect','oidc-full-name-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','picture','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','phone number','openid-connect','oidc-usermodel-attribute-mapper',NULL,'5619f20a-e951-410e-af88-38084f0f498b'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','locale','openid-connect','oidc-usermodel-attribute-mapper','e12fceb4-c28d-4e75-8f26-48fe7824c110',NULL),('8d75cb61-a259-4526-a937-2193ac6f14af','phone number verified','openid-connect','oidc-usermodel-attribute-mapper',NULL,'453424f4-1a6e-412f-ace0-7c92331cc1e4'),('9094dbb7-675c-4667-a5af-d7009b25f1d2','audience resolve','openid-connect','oidc-audience-resolve-mapper',NULL,'c2f2e082-e87c-419d-af80-8803991286f6'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','gender','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('93a2b348-59fe-48a0-b713-833dfd7fcc75','client roles','openid-connect','oidc-usermodel-client-role-mapper',NULL,'c2f2e082-e87c-419d-af80-8803991286f6'),('93ce9d61-9891-406d-908a-41272294eb58','nickname','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('97249ac8-bc18-4312-8f19-0d4d3a37c8e4','full name','openid-connect','oidc-full-name-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','address','openid-connect','oidc-address-mapper',NULL,'9d152c27-e725-4a7e-9984-d18e62b07911'),('991fa8cb-ce3f-455d-9faf-91d5cd367d87','organization','saml','saml-organization-membership-mapper',NULL,'3ae50474-5f28-4562-baa3-4ee4463c183f'),('9c0bea43-2baa-4f2c-aaca-2396e0066395','realm roles','openid-connect','oidc-usermodel-realm-role-mapper',NULL,'44bc3d1e-d675-4a76-b1a8-c976fbd843cd'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','family name','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('9eba10a6-d829-43c1-9a34-571cdaea812c','locale','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('9f9ad4d3-0ed8-405c-99e2-b9bc99b04299','Client Host','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'a4a4fdcf-e5be-4d5e-8874-2e6ddfc7cd34'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','locale','openid-connect','oidc-usermodel-attribute-mapper','7793a774-b465-4b0b-97d1-08840aa08c9a',NULL),('a53d28c0-d504-4b4c-bb53-ab3203d6f104','Client Host','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'e94865e0-603b-4582-b7d4-90151939f139'),('afbd5e95-8fcb-4d96-9c00-75a491e440cc','audience resolve','openid-connect','oidc-audience-resolve-mapper','703aca18-8a9e-44ea-bf88-648ae965c214',NULL),('afc75085-aefb-446e-9f67-9eb26837d8a5','profile','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','address','openid-connect','oidc-address-mapper',NULL,'517304c6-4e73-4a1a-ae1c-89de779c6d86'),('b2d69007-eb61-4057-9fc0-a2d18c8b6a16','client roles','openid-connect','oidc-usermodel-client-role-mapper',NULL,'44bc3d1e-d675-4a76-b1a8-c976fbd843cd'),('b4c7e8b4-ebaf-44d9-96fb-ec9d719970fd','sub','openid-connect','oidc-sub-mapper',NULL,'a1327838-8323-46e5-b4c2-85da961df597'),('b5807ac0-1e61-471d-81bd-86c34245dc5d','audience resolve','openid-connect','oidc-audience-resolve-mapper','a57d63c3-4d3c-40d6-a086-9d43cd4fedd5',NULL),('b7646abb-e3ce-460e-b1e7-eb07513892fc','auth_time','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'e61927e1-ec5d-4137-8617-31b436b5c9be'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','username','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','email verified','openid-connect','oidc-usermodel-property-mapper',NULL,'d59644b7-45b9-4c40-9004-ad0f4c922689'),('cdbfaf63-5fe1-4c0a-8823-90a4dfc529cd','Client ID','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'a4a4fdcf-e5be-4d5e-8874-2e6ddfc7cd34'),('d77efb85-6251-4161-89b1-2e302e511416','realm roles','openid-connect','oidc-usermodel-realm-role-mapper',NULL,'c2f2e082-e87c-419d-af80-8803991286f6'),('dd74013f-0834-4c28-8f12-32de7b3f799b','updated at','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','gender','openid-connect','oidc-usermodel-attribute-mapper',NULL,'0a69cb43-9249-465f-8bea-e8097fd91e24'),('e3c8da74-40f4-4fbe-bb8c-1893e0bc7982','Client ID','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'e94865e0-603b-4582-b7d4-90151939f139'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','email verified','openid-connect','oidc-usermodel-property-mapper',NULL,'6c1afe15-15bd-436c-b923-abfdc3701b4e'),('f113be9e-3ea6-4871-800b-6e2f1d43446c','Client IP Address','openid-connect','oidc-usersessionmodel-note-mapper',NULL,'a4a4fdcf-e5be-4d5e-8874-2e6ddfc7cd34'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','profile','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','email','openid-connect','oidc-usermodel-attribute-mapper',NULL,'6c1afe15-15bd-436c-b923-abfdc3701b4e'),('f9cc8a5a-5011-4045-b549-57ac2b221254','role list','saml','saml-role-list-mapper',NULL,'5139b7db-f531-479a-91b1-af0146f86a7c'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','locale','openid-connect','oidc-usermodel-attribute-mapper',NULL,'fdd5034a-7fc4-499e-9eaa-3544210e2dd1'),('fcafc081-4a99-4c40-95e5-cee60b8eef3e','organization','openid-connect','oidc-organization-membership-mapper',NULL,'eb32d23a-ac59-4e89-8ecb-e0b7a1d5d3e7'),('fd661af0-cc42-4163-af04-db002afa50e1','upn','openid-connect','oidc-usermodel-attribute-mapper',NULL,'47a8d258-2f7f-4d4f-9cb3-e67650ed8a4f');
/*!40000 ALTER TABLE `PROTOCOL_MAPPER` ENABLE KEYS */;

--
-- Table structure for table `PROTOCOL_MAPPER_CONFIG`
--

DROP TABLE IF EXISTS `PROTOCOL_MAPPER_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PROTOCOL_MAPPER_CONFIG` (
  `PROTOCOL_MAPPER_ID` varchar(36) NOT NULL,
  `VALUE` longtext,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`PROTOCOL_MAPPER_ID`,`NAME`),
  CONSTRAINT `FK_PMCONFIG` FOREIGN KEY (`PROTOCOL_MAPPER_ID`) REFERENCES `PROTOCOL_MAPPER` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PROTOCOL_MAPPER_CONFIG`
--

/*!40000 ALTER TABLE `PROTOCOL_MAPPER_CONFIG` DISABLE KEYS */;
INSERT INTO `PROTOCOL_MAPPER_CONFIG` VALUES ('055a5c5f-6641-4cc3-90e4-238053851a49','true','access.token.claim'),('055a5c5f-6641-4cc3-90e4-238053851a49','true','introspection.token.claim'),('092c4112-ab38-4c4a-b00e-4737c959337a','true','access.token.claim'),('092c4112-ab38-4c4a-b00e-4737c959337a','phone_number_verified','claim.name'),('092c4112-ab38-4c4a-b00e-4737c959337a','true','id.token.claim'),('092c4112-ab38-4c4a-b00e-4737c959337a','true','introspection.token.claim'),('092c4112-ab38-4c4a-b00e-4737c959337a','boolean','jsonType.label'),('092c4112-ab38-4c4a-b00e-4737c959337a','phoneNumberVerified','user.attribute'),('092c4112-ab38-4c4a-b00e-4737c959337a','true','userinfo.token.claim'),('0d3986d9-9945-42ea-9f36-4c4b2c14c637','true','access.token.claim'),('0d3986d9-9945-42ea-9f36-4c4b2c14c637','true','introspection.token.claim'),('10c60ef0-1678-43bb-8dce-71940370a216','true','access.token.claim'),('10c60ef0-1678-43bb-8dce-71940370a216','family_name','claim.name'),('10c60ef0-1678-43bb-8dce-71940370a216','true','id.token.claim'),('10c60ef0-1678-43bb-8dce-71940370a216','true','introspection.token.claim'),('10c60ef0-1678-43bb-8dce-71940370a216','String','jsonType.label'),('10c60ef0-1678-43bb-8dce-71940370a216','lastName','user.attribute'),('10c60ef0-1678-43bb-8dce-71940370a216','true','userinfo.token.claim'),('11c146da-9791-4b43-a132-d3eea8ddb12e','true','access.token.claim'),('11c146da-9791-4b43-a132-d3eea8ddb12e','middle_name','claim.name'),('11c146da-9791-4b43-a132-d3eea8ddb12e','true','id.token.claim'),('11c146da-9791-4b43-a132-d3eea8ddb12e','true','introspection.token.claim'),('11c146da-9791-4b43-a132-d3eea8ddb12e','String','jsonType.label'),('11c146da-9791-4b43-a132-d3eea8ddb12e','middleName','user.attribute'),('11c146da-9791-4b43-a132-d3eea8ddb12e','true','userinfo.token.claim'),('1da16af7-a9dc-454e-8c79-517bf9df59e6','true','access.token.claim'),('1da16af7-a9dc-454e-8c79-517bf9df59e6','true','introspection.token.claim'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','true','access.token.claim'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','picture','claim.name'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','true','id.token.claim'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','true','introspection.token.claim'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','String','jsonType.label'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','picture','user.attribute'),('20a50a26-6cf3-4a51-99f3-3ce1ed242686','true','userinfo.token.claim'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','true','access.token.claim'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','updated_at','claim.name'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','true','id.token.claim'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','true','introspection.token.claim'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','long','jsonType.label'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','updatedAt','user.attribute'),('22533a88-0e7e-4d73-b4f8-64e7bf1ed9ce','true','userinfo.token.claim'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','true','access.token.claim'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','birthdate','claim.name'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','true','id.token.claim'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','true','introspection.token.claim'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','String','jsonType.label'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','birthdate','user.attribute'),('2407dfd0-687a-4475-9c68-17ba2eeb81ba','true','userinfo.token.claim'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','true','access.token.claim'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','upn','claim.name'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','true','id.token.claim'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','true','introspection.token.claim'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','String','jsonType.label'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','username','user.attribute'),('24fa9914-6f17-4b3f-b5fa-590ed6d0d2c0','true','userinfo.token.claim'),('25f5665a-e12a-493d-8541-593bd82ba862','true','access.token.claim'),('25f5665a-e12a-493d-8541-593bd82ba862','given_name','claim.name'),('25f5665a-e12a-493d-8541-593bd82ba862','true','id.token.claim'),('25f5665a-e12a-493d-8541-593bd82ba862','true','introspection.token.claim'),('25f5665a-e12a-493d-8541-593bd82ba862','String','jsonType.label'),('25f5665a-e12a-493d-8541-593bd82ba862','firstName','user.attribute'),('25f5665a-e12a-493d-8541-593bd82ba862','true','userinfo.token.claim'),('2caeccff-aa98-4236-9729-3c8923856a23','true','access.token.claim'),('2caeccff-aa98-4236-9729-3c8923856a23','true','id.token.claim'),('2caeccff-aa98-4236-9729-3c8923856a23','true','introspection.token.claim'),('3d0c686e-9d3c-4ae4-bf2c-74cec9cf4f3e','true','access.token.claim'),('3d0c686e-9d3c-4ae4-bf2c-74cec9cf4f3e','organization','claim.name'),('3d0c686e-9d3c-4ae4-bf2c-74cec9cf4f3e','true','id.token.claim'),('3d0c686e-9d3c-4ae4-bf2c-74cec9cf4f3e','true','introspection.token.claim'),('3d0c686e-9d3c-4ae4-bf2c-74cec9cf4f3e','String','jsonType.label'),('3d0c686e-9d3c-4ae4-bf2c-74cec9cf4f3e','true','multivalued'),('400b3bdb-7310-4c6b-b74f-a53915bde681','true','access.token.claim'),('400b3bdb-7310-4c6b-b74f-a53915bde681','clientAddress','claim.name'),('400b3bdb-7310-4c6b-b74f-a53915bde681','true','id.token.claim'),('400b3bdb-7310-4c6b-b74f-a53915bde681','true','introspection.token.claim'),('400b3bdb-7310-4c6b-b74f-a53915bde681','String','jsonType.label'),('400b3bdb-7310-4c6b-b74f-a53915bde681','clientAddress','user.session.note'),('411506ad-6196-430c-8b42-1ba0723a03b8','true','access.token.claim'),('411506ad-6196-430c-8b42-1ba0723a03b8','groups','claim.name'),('411506ad-6196-430c-8b42-1ba0723a03b8','true','id.token.claim'),('411506ad-6196-430c-8b42-1ba0723a03b8','true','introspection.token.claim'),('411506ad-6196-430c-8b42-1ba0723a03b8','String','jsonType.label'),('411506ad-6196-430c-8b42-1ba0723a03b8','true','multivalued'),('411506ad-6196-430c-8b42-1ba0723a03b8','foo','user.attribute'),('4251d783-fd2a-4fcd-9dda-b0bdaced2507','true','access.token.claim'),('4251d783-fd2a-4fcd-9dda-b0bdaced2507','true','introspection.token.claim'),('44dc18c4-a197-4494-b4d8-0672458f69c4','true','access.token.claim'),('44dc18c4-a197-4494-b4d8-0672458f69c4','website','claim.name'),('44dc18c4-a197-4494-b4d8-0672458f69c4','true','id.token.claim'),('44dc18c4-a197-4494-b4d8-0672458f69c4','true','introspection.token.claim'),('44dc18c4-a197-4494-b4d8-0672458f69c4','String','jsonType.label'),('44dc18c4-a197-4494-b4d8-0672458f69c4','website','user.attribute'),('44dc18c4-a197-4494-b4d8-0672458f69c4','true','userinfo.token.claim'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','true','access.token.claim'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','phone_number','claim.name'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','true','id.token.claim'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','true','introspection.token.claim'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','String','jsonType.label'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','phoneNumber','user.attribute'),('46c4c0f9-84fc-4ebc-91eb-495a88abc1f7','true','userinfo.token.claim'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','true','access.token.claim'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','birthdate','claim.name'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','true','id.token.claim'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','true','introspection.token.claim'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','String','jsonType.label'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','birthdate','user.attribute'),('4a5d0cc6-3763-4eeb-87ac-e0b824dae3e3','true','userinfo.token.claim'),('4bb6c4fe-8b04-4bd8-94c9-969edd50749a','true','access.token.claim'),('4bb6c4fe-8b04-4bd8-94c9-969edd50749a','auth_time','claim.name'),('4bb6c4fe-8b04-4bd8-94c9-969edd50749a','true','id.token.claim'),('4bb6c4fe-8b04-4bd8-94c9-969edd50749a','true','introspection.token.claim'),('4bb6c4fe-8b04-4bd8-94c9-969edd50749a','long','jsonType.label'),('4bb6c4fe-8b04-4bd8-94c9-969edd50749a','AUTH_TIME','user.session.note'),('505a236a-0dbb-4eec-a431-006bc72ce764','true','access.token.claim'),('505a236a-0dbb-4eec-a431-006bc72ce764','zoneinfo','claim.name'),('505a236a-0dbb-4eec-a431-006bc72ce764','true','id.token.claim'),('505a236a-0dbb-4eec-a431-006bc72ce764','true','introspection.token.claim'),('505a236a-0dbb-4eec-a431-006bc72ce764','String','jsonType.label'),('505a236a-0dbb-4eec-a431-006bc72ce764','zoneinfo','user.attribute'),('505a236a-0dbb-4eec-a431-006bc72ce764','true','userinfo.token.claim'),('5063bb39-a664-48ac-864d-48779221b653','true','access.token.claim'),('5063bb39-a664-48ac-864d-48779221b653','preferred_username','claim.name'),('5063bb39-a664-48ac-864d-48779221b653','true','id.token.claim'),('5063bb39-a664-48ac-864d-48779221b653','true','introspection.token.claim'),('5063bb39-a664-48ac-864d-48779221b653','String','jsonType.label'),('5063bb39-a664-48ac-864d-48779221b653','username','user.attribute'),('5063bb39-a664-48ac-864d-48779221b653','true','userinfo.token.claim'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','true','access.token.claim'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','given_name','claim.name'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','true','id.token.claim'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','true','introspection.token.claim'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','String','jsonType.label'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','firstName','user.attribute'),('51ca8ceb-66a6-4c6e-abbf-ee0052afccca','true','userinfo.token.claim'),('593da648-193d-447c-9ba3-94a07fcf6c55','Role','attribute.name'),('593da648-193d-447c-9ba3-94a07fcf6c55','Basic','attribute.nameformat'),('593da648-193d-447c-9ba3-94a07fcf6c55','false','single'),('596cb309-0292-4989-b434-9edf648f3efc','true','access.token.claim'),('596cb309-0292-4989-b434-9edf648f3efc','groups','claim.name'),('596cb309-0292-4989-b434-9edf648f3efc','true','id.token.claim'),('596cb309-0292-4989-b434-9edf648f3efc','true','introspection.token.claim'),('596cb309-0292-4989-b434-9edf648f3efc','String','jsonType.label'),('596cb309-0292-4989-b434-9edf648f3efc','true','multivalued'),('596cb309-0292-4989-b434-9edf648f3efc','foo','user.attribute'),('689ec476-fded-4c71-b583-368473b661be','true','access.token.claim'),('689ec476-fded-4c71-b583-368473b661be','true','id.token.claim'),('689ec476-fded-4c71-b583-368473b661be','true','introspection.token.claim'),('693040b1-c49e-4ae7-baaa-777a554e635b','true','access.token.claim'),('693040b1-c49e-4ae7-baaa-777a554e635b','email','claim.name'),('693040b1-c49e-4ae7-baaa-777a554e635b','true','id.token.claim'),('693040b1-c49e-4ae7-baaa-777a554e635b','true','introspection.token.claim'),('693040b1-c49e-4ae7-baaa-777a554e635b','String','jsonType.label'),('693040b1-c49e-4ae7-baaa-777a554e635b','email','user.attribute'),('693040b1-c49e-4ae7-baaa-777a554e635b','true','userinfo.token.claim'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','true','access.token.claim'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','website','claim.name'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','true','id.token.claim'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','true','introspection.token.claim'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','String','jsonType.label'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','website','user.attribute'),('6cf7ab0a-ea57-48b3-b4ff-dbddcaea121b','true','userinfo.token.claim'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','true','access.token.claim'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','middle_name','claim.name'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','true','id.token.claim'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','true','introspection.token.claim'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','String','jsonType.label'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','middleName','user.attribute'),('6db7d2e3-4316-41b2-8ddf-b367cf17742c','true','userinfo.token.claim'),('70dcce46-d2de-41c1-afea-1ebe242a5507','true','access.token.claim'),('70dcce46-d2de-41c1-afea-1ebe242a5507','nickname','claim.name'),('70dcce46-d2de-41c1-afea-1ebe242a5507','true','id.token.claim'),('70dcce46-d2de-41c1-afea-1ebe242a5507','true','introspection.token.claim'),('70dcce46-d2de-41c1-afea-1ebe242a5507','String','jsonType.label'),('70dcce46-d2de-41c1-afea-1ebe242a5507','nickname','user.attribute'),('70dcce46-d2de-41c1-afea-1ebe242a5507','true','userinfo.token.claim'),('754bb346-6796-4460-8ad6-5aac0c34275b','true','access.token.claim'),('754bb346-6796-4460-8ad6-5aac0c34275b','zoneinfo','claim.name'),('754bb346-6796-4460-8ad6-5aac0c34275b','true','id.token.claim'),('754bb346-6796-4460-8ad6-5aac0c34275b','true','introspection.token.claim'),('754bb346-6796-4460-8ad6-5aac0c34275b','String','jsonType.label'),('754bb346-6796-4460-8ad6-5aac0c34275b','zoneinfo','user.attribute'),('754bb346-6796-4460-8ad6-5aac0c34275b','true','userinfo.token.claim'),('77ed86f8-cf6e-47ed-89f1-e87161c9180c','true','access.token.claim'),('77ed86f8-cf6e-47ed-89f1-e87161c9180c','true','id.token.claim'),('77ed86f8-cf6e-47ed-89f1-e87161c9180c','true','introspection.token.claim'),('77ed86f8-cf6e-47ed-89f1-e87161c9180c','true','userinfo.token.claim'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','true','access.token.claim'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','picture','claim.name'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','true','id.token.claim'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','true','introspection.token.claim'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','String','jsonType.label'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','picture','user.attribute'),('808b2d05-0ae6-4ee2-a78b-c00fa67bde35','true','userinfo.token.claim'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','true','access.token.claim'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','phone_number','claim.name'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','true','id.token.claim'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','true','introspection.token.claim'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','String','jsonType.label'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','phoneNumber','user.attribute'),('8bddafd2-9d85-469d-a78f-17359c5abbe4','true','userinfo.token.claim'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','true','access.token.claim'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','locale','claim.name'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','true','id.token.claim'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','true','introspection.token.claim'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','String','jsonType.label'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','locale','user.attribute'),('8d4a8dcd-1647-4577-b833-74dd3972b8d9','true','userinfo.token.claim'),('8d75cb61-a259-4526-a937-2193ac6f14af','true','access.token.claim'),('8d75cb61-a259-4526-a937-2193ac6f14af','phone_number_verified','claim.name'),('8d75cb61-a259-4526-a937-2193ac6f14af','true','id.token.claim'),('8d75cb61-a259-4526-a937-2193ac6f14af','true','introspection.token.claim'),('8d75cb61-a259-4526-a937-2193ac6f14af','boolean','jsonType.label'),('8d75cb61-a259-4526-a937-2193ac6f14af','phoneNumberVerified','user.attribute'),('8d75cb61-a259-4526-a937-2193ac6f14af','true','userinfo.token.claim'),('9094dbb7-675c-4667-a5af-d7009b25f1d2','true','access.token.claim'),('9094dbb7-675c-4667-a5af-d7009b25f1d2','true','introspection.token.claim'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','true','access.token.claim'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','gender','claim.name'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','true','id.token.claim'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','true','introspection.token.claim'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','String','jsonType.label'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','gender','user.attribute'),('92e7004f-04f2-46e8-a90d-5d0cc4fecb53','true','userinfo.token.claim'),('93a2b348-59fe-48a0-b713-833dfd7fcc75','true','access.token.claim'),('93a2b348-59fe-48a0-b713-833dfd7fcc75','resource_access.${client_id}.roles','claim.name'),('93a2b348-59fe-48a0-b713-833dfd7fcc75','true','introspection.token.claim'),('93a2b348-59fe-48a0-b713-833dfd7fcc75','String','jsonType.label'),('93a2b348-59fe-48a0-b713-833dfd7fcc75','true','multivalued'),('93a2b348-59fe-48a0-b713-833dfd7fcc75','foo','user.attribute'),('93ce9d61-9891-406d-908a-41272294eb58','true','access.token.claim'),('93ce9d61-9891-406d-908a-41272294eb58','nickname','claim.name'),('93ce9d61-9891-406d-908a-41272294eb58','true','id.token.claim'),('93ce9d61-9891-406d-908a-41272294eb58','true','introspection.token.claim'),('93ce9d61-9891-406d-908a-41272294eb58','String','jsonType.label'),('93ce9d61-9891-406d-908a-41272294eb58','nickname','user.attribute'),('93ce9d61-9891-406d-908a-41272294eb58','true','userinfo.token.claim'),('97249ac8-bc18-4312-8f19-0d4d3a37c8e4','true','access.token.claim'),('97249ac8-bc18-4312-8f19-0d4d3a37c8e4','true','id.token.claim'),('97249ac8-bc18-4312-8f19-0d4d3a37c8e4','true','introspection.token.claim'),('97249ac8-bc18-4312-8f19-0d4d3a37c8e4','true','userinfo.token.claim'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','true','access.token.claim'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','true','id.token.claim'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','true','introspection.token.claim'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','country','user.attribute.country'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','formatted','user.attribute.formatted'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','locality','user.attribute.locality'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','postal_code','user.attribute.postal_code'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','region','user.attribute.region'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','street','user.attribute.street'),('97618fd5-9f02-4dc0-8b30-b7dfc7ed251e','true','userinfo.token.claim'),('9c0bea43-2baa-4f2c-aaca-2396e0066395','true','access.token.claim'),('9c0bea43-2baa-4f2c-aaca-2396e0066395','realm_access.roles','claim.name'),('9c0bea43-2baa-4f2c-aaca-2396e0066395','true','introspection.token.claim'),('9c0bea43-2baa-4f2c-aaca-2396e0066395','String','jsonType.label'),('9c0bea43-2baa-4f2c-aaca-2396e0066395','true','multivalued'),('9c0bea43-2baa-4f2c-aaca-2396e0066395','foo','user.attribute'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','true','access.token.claim'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','family_name','claim.name'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','true','id.token.claim'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','true','introspection.token.claim'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','String','jsonType.label'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','lastName','user.attribute'),('9d7f5bbd-2ee0-4e0b-bb80-7a87bf7b2b51','true','userinfo.token.claim'),('9eba10a6-d829-43c1-9a34-571cdaea812c','true','access.token.claim'),('9eba10a6-d829-43c1-9a34-571cdaea812c','locale','claim.name'),('9eba10a6-d829-43c1-9a34-571cdaea812c','true','id.token.claim'),('9eba10a6-d829-43c1-9a34-571cdaea812c','true','introspection.token.claim'),('9eba10a6-d829-43c1-9a34-571cdaea812c','String','jsonType.label'),('9eba10a6-d829-43c1-9a34-571cdaea812c','locale','user.attribute'),('9eba10a6-d829-43c1-9a34-571cdaea812c','true','userinfo.token.claim'),('9f9ad4d3-0ed8-405c-99e2-b9bc99b04299','true','access.token.claim'),('9f9ad4d3-0ed8-405c-99e2-b9bc99b04299','clientHost','claim.name'),('9f9ad4d3-0ed8-405c-99e2-b9bc99b04299','true','id.token.claim'),('9f9ad4d3-0ed8-405c-99e2-b9bc99b04299','true','introspection.token.claim'),('9f9ad4d3-0ed8-405c-99e2-b9bc99b04299','String','jsonType.label'),('9f9ad4d3-0ed8-405c-99e2-b9bc99b04299','clientHost','user.session.note'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','true','access.token.claim'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','locale','claim.name'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','true','id.token.claim'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','true','introspection.token.claim'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','String','jsonType.label'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','locale','user.attribute'),('a02bfac2-2bdb-4cb0-93c4-73a55824aba8','true','userinfo.token.claim'),('a53d28c0-d504-4b4c-bb53-ab3203d6f104','true','access.token.claim'),('a53d28c0-d504-4b4c-bb53-ab3203d6f104','clientHost','claim.name'),('a53d28c0-d504-4b4c-bb53-ab3203d6f104','true','id.token.claim'),('a53d28c0-d504-4b4c-bb53-ab3203d6f104','true','introspection.token.claim'),('a53d28c0-d504-4b4c-bb53-ab3203d6f104','String','jsonType.label'),('a53d28c0-d504-4b4c-bb53-ab3203d6f104','clientHost','user.session.note'),('afc75085-aefb-446e-9f67-9eb26837d8a5','true','access.token.claim'),('afc75085-aefb-446e-9f67-9eb26837d8a5','profile','claim.name'),('afc75085-aefb-446e-9f67-9eb26837d8a5','true','id.token.claim'),('afc75085-aefb-446e-9f67-9eb26837d8a5','true','introspection.token.claim'),('afc75085-aefb-446e-9f67-9eb26837d8a5','String','jsonType.label'),('afc75085-aefb-446e-9f67-9eb26837d8a5','profile','user.attribute'),('afc75085-aefb-446e-9f67-9eb26837d8a5','true','userinfo.token.claim'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','true','access.token.claim'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','true','id.token.claim'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','true','introspection.token.claim'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','country','user.attribute.country'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','formatted','user.attribute.formatted'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','locality','user.attribute.locality'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','postal_code','user.attribute.postal_code'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','region','user.attribute.region'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','street','user.attribute.street'),('b1dcfe9e-abd7-4a7f-bc61-3eee6e37d07b','true','userinfo.token.claim'),('b2d69007-eb61-4057-9fc0-a2d18c8b6a16','true','access.token.claim'),('b2d69007-eb61-4057-9fc0-a2d18c8b6a16','resource_access.${client_id}.roles','claim.name'),('b2d69007-eb61-4057-9fc0-a2d18c8b6a16','true','introspection.token.claim'),('b2d69007-eb61-4057-9fc0-a2d18c8b6a16','String','jsonType.label'),('b2d69007-eb61-4057-9fc0-a2d18c8b6a16','true','multivalued'),('b2d69007-eb61-4057-9fc0-a2d18c8b6a16','foo','user.attribute'),('b4c7e8b4-ebaf-44d9-96fb-ec9d719970fd','true','access.token.claim'),('b4c7e8b4-ebaf-44d9-96fb-ec9d719970fd','true','introspection.token.claim'),('b7646abb-e3ce-460e-b1e7-eb07513892fc','true','access.token.claim'),('b7646abb-e3ce-460e-b1e7-eb07513892fc','auth_time','claim.name'),('b7646abb-e3ce-460e-b1e7-eb07513892fc','true','id.token.claim'),('b7646abb-e3ce-460e-b1e7-eb07513892fc','true','introspection.token.claim'),('b7646abb-e3ce-460e-b1e7-eb07513892fc','long','jsonType.label'),('b7646abb-e3ce-460e-b1e7-eb07513892fc','AUTH_TIME','user.session.note'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','true','access.token.claim'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','preferred_username','claim.name'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','true','id.token.claim'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','true','introspection.token.claim'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','String','jsonType.label'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','username','user.attribute'),('c5d27d79-53d1-413e-aa34-55d10ff980ae','true','userinfo.token.claim'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','true','access.token.claim'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','email_verified','claim.name'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','true','id.token.claim'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','true','introspection.token.claim'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','boolean','jsonType.label'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','emailVerified','user.attribute'),('cabdc937-29ea-48ce-beb0-483bfb8f7040','true','userinfo.token.claim'),('cdbfaf63-5fe1-4c0a-8823-90a4dfc529cd','true','access.token.claim'),('cdbfaf63-5fe1-4c0a-8823-90a4dfc529cd','client_id','claim.name'),('cdbfaf63-5fe1-4c0a-8823-90a4dfc529cd','true','id.token.claim'),('cdbfaf63-5fe1-4c0a-8823-90a4dfc529cd','true','introspection.token.claim'),('cdbfaf63-5fe1-4c0a-8823-90a4dfc529cd','String','jsonType.label'),('cdbfaf63-5fe1-4c0a-8823-90a4dfc529cd','client_id','user.session.note'),('d77efb85-6251-4161-89b1-2e302e511416','true','access.token.claim'),('d77efb85-6251-4161-89b1-2e302e511416','realm_access.roles','claim.name'),('d77efb85-6251-4161-89b1-2e302e511416','true','introspection.token.claim'),('d77efb85-6251-4161-89b1-2e302e511416','String','jsonType.label'),('d77efb85-6251-4161-89b1-2e302e511416','true','multivalued'),('d77efb85-6251-4161-89b1-2e302e511416','foo','user.attribute'),('dd74013f-0834-4c28-8f12-32de7b3f799b','true','access.token.claim'),('dd74013f-0834-4c28-8f12-32de7b3f799b','updated_at','claim.name'),('dd74013f-0834-4c28-8f12-32de7b3f799b','true','id.token.claim'),('dd74013f-0834-4c28-8f12-32de7b3f799b','true','introspection.token.claim'),('dd74013f-0834-4c28-8f12-32de7b3f799b','long','jsonType.label'),('dd74013f-0834-4c28-8f12-32de7b3f799b','updatedAt','user.attribute'),('dd74013f-0834-4c28-8f12-32de7b3f799b','true','userinfo.token.claim'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','true','access.token.claim'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','gender','claim.name'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','true','id.token.claim'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','true','introspection.token.claim'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','String','jsonType.label'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','gender','user.attribute'),('dfa729d2-b02e-4107-9691-254b96d0e5d0','true','userinfo.token.claim'),('e3c8da74-40f4-4fbe-bb8c-1893e0bc7982','true','access.token.claim'),('e3c8da74-40f4-4fbe-bb8c-1893e0bc7982','client_id','claim.name'),('e3c8da74-40f4-4fbe-bb8c-1893e0bc7982','true','id.token.claim'),('e3c8da74-40f4-4fbe-bb8c-1893e0bc7982','true','introspection.token.claim'),('e3c8da74-40f4-4fbe-bb8c-1893e0bc7982','String','jsonType.label'),('e3c8da74-40f4-4fbe-bb8c-1893e0bc7982','client_id','user.session.note'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','true','access.token.claim'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','email_verified','claim.name'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','true','id.token.claim'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','true','introspection.token.claim'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','boolean','jsonType.label'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','emailVerified','user.attribute'),('efa2240b-e594-40e5-b8bd-7bf46b4cefd6','true','userinfo.token.claim'),('f113be9e-3ea6-4871-800b-6e2f1d43446c','true','access.token.claim'),('f113be9e-3ea6-4871-800b-6e2f1d43446c','clientAddress','claim.name'),('f113be9e-3ea6-4871-800b-6e2f1d43446c','true','id.token.claim'),('f113be9e-3ea6-4871-800b-6e2f1d43446c','true','introspection.token.claim'),('f113be9e-3ea6-4871-800b-6e2f1d43446c','String','jsonType.label'),('f113be9e-3ea6-4871-800b-6e2f1d43446c','clientAddress','user.session.note'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','true','access.token.claim'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','profile','claim.name'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','true','id.token.claim'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','true','introspection.token.claim'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','String','jsonType.label'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','profile','user.attribute'),('f3e6ba6a-372c-4811-8416-7db0a8a21d03','true','userinfo.token.claim'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','true','access.token.claim'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','email','claim.name'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','true','id.token.claim'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','true','introspection.token.claim'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','String','jsonType.label'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','email','user.attribute'),('f4c98d11-bef3-4acd-b8fd-928e3d25fa79','true','userinfo.token.claim'),('f9cc8a5a-5011-4045-b549-57ac2b221254','Role','attribute.name'),('f9cc8a5a-5011-4045-b549-57ac2b221254','Basic','attribute.nameformat'),('f9cc8a5a-5011-4045-b549-57ac2b221254','false','single'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','true','access.token.claim'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','locale','claim.name'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','true','id.token.claim'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','true','introspection.token.claim'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','String','jsonType.label'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','locale','user.attribute'),('fbd2577d-3a17-46dd-8f08-c7922e7fa794','true','userinfo.token.claim'),('fcafc081-4a99-4c40-95e5-cee60b8eef3e','true','access.token.claim'),('fcafc081-4a99-4c40-95e5-cee60b8eef3e','organization','claim.name'),('fcafc081-4a99-4c40-95e5-cee60b8eef3e','true','id.token.claim'),('fcafc081-4a99-4c40-95e5-cee60b8eef3e','true','introspection.token.claim'),('fcafc081-4a99-4c40-95e5-cee60b8eef3e','String','jsonType.label'),('fcafc081-4a99-4c40-95e5-cee60b8eef3e','true','multivalued'),('fd661af0-cc42-4163-af04-db002afa50e1','true','access.token.claim'),('fd661af0-cc42-4163-af04-db002afa50e1','upn','claim.name'),('fd661af0-cc42-4163-af04-db002afa50e1','true','id.token.claim'),('fd661af0-cc42-4163-af04-db002afa50e1','true','introspection.token.claim'),('fd661af0-cc42-4163-af04-db002afa50e1','String','jsonType.label'),('fd661af0-cc42-4163-af04-db002afa50e1','username','user.attribute'),('fd661af0-cc42-4163-af04-db002afa50e1','true','userinfo.token.claim');
/*!40000 ALTER TABLE `PROTOCOL_MAPPER_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `REALM`
--

DROP TABLE IF EXISTS `REALM`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM` (
  `ID` varchar(36) NOT NULL,
  `ACCESS_CODE_LIFESPAN` int DEFAULT NULL,
  `USER_ACTION_LIFESPAN` int DEFAULT NULL,
  `ACCESS_TOKEN_LIFESPAN` int DEFAULT NULL,
  `ACCOUNT_THEME` varchar(255) DEFAULT NULL,
  `ADMIN_THEME` varchar(255) DEFAULT NULL,
  `EMAIL_THEME` varchar(255) DEFAULT NULL,
  `ENABLED` tinyint NOT NULL DEFAULT '0',
  `EVENTS_ENABLED` tinyint NOT NULL DEFAULT '0',
  `EVENTS_EXPIRATION` bigint DEFAULT NULL,
  `LOGIN_THEME` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `NOT_BEFORE` int DEFAULT NULL,
  `PASSWORD_POLICY` text,
  `REGISTRATION_ALLOWED` tinyint NOT NULL DEFAULT '0',
  `REMEMBER_ME` tinyint NOT NULL DEFAULT '0',
  `RESET_PASSWORD_ALLOWED` tinyint NOT NULL DEFAULT '0',
  `SOCIAL` tinyint NOT NULL DEFAULT '0',
  `SSL_REQUIRED` varchar(255) DEFAULT NULL,
  `SSO_IDLE_TIMEOUT` int DEFAULT NULL,
  `SSO_MAX_LIFESPAN` int DEFAULT NULL,
  `UPDATE_PROFILE_ON_SOC_LOGIN` tinyint NOT NULL DEFAULT '0',
  `VERIFY_EMAIL` tinyint NOT NULL DEFAULT '0',
  `MASTER_ADMIN_CLIENT` varchar(36) DEFAULT NULL,
  `LOGIN_LIFESPAN` int DEFAULT NULL,
  `INTERNATIONALIZATION_ENABLED` tinyint NOT NULL DEFAULT '0',
  `DEFAULT_LOCALE` varchar(255) DEFAULT NULL,
  `REG_EMAIL_AS_USERNAME` tinyint NOT NULL DEFAULT '0',
  `ADMIN_EVENTS_ENABLED` tinyint NOT NULL DEFAULT '0',
  `ADMIN_EVENTS_DETAILS_ENABLED` tinyint NOT NULL DEFAULT '0',
  `EDIT_USERNAME_ALLOWED` tinyint NOT NULL DEFAULT '0',
  `OTP_POLICY_COUNTER` int DEFAULT '0',
  `OTP_POLICY_WINDOW` int DEFAULT '1',
  `OTP_POLICY_PERIOD` int DEFAULT '30',
  `OTP_POLICY_DIGITS` int DEFAULT '6',
  `OTP_POLICY_ALG` varchar(36) DEFAULT 'HmacSHA1',
  `OTP_POLICY_TYPE` varchar(36) DEFAULT 'totp',
  `BROWSER_FLOW` varchar(36) DEFAULT NULL,
  `REGISTRATION_FLOW` varchar(36) DEFAULT NULL,
  `DIRECT_GRANT_FLOW` varchar(36) DEFAULT NULL,
  `RESET_CREDENTIALS_FLOW` varchar(36) DEFAULT NULL,
  `CLIENT_AUTH_FLOW` varchar(36) DEFAULT NULL,
  `OFFLINE_SESSION_IDLE_TIMEOUT` int DEFAULT '0',
  `REVOKE_REFRESH_TOKEN` tinyint NOT NULL DEFAULT '0',
  `ACCESS_TOKEN_LIFE_IMPLICIT` int DEFAULT '0',
  `LOGIN_WITH_EMAIL_ALLOWED` tinyint NOT NULL DEFAULT '1',
  `DUPLICATE_EMAILS_ALLOWED` tinyint NOT NULL DEFAULT '0',
  `DOCKER_AUTH_FLOW` varchar(36) DEFAULT NULL,
  `REFRESH_TOKEN_MAX_REUSE` int DEFAULT '0',
  `ALLOW_USER_MANAGED_ACCESS` tinyint NOT NULL DEFAULT '0',
  `SSO_MAX_LIFESPAN_REMEMBER_ME` int NOT NULL,
  `SSO_IDLE_TIMEOUT_REMEMBER_ME` int NOT NULL,
  `DEFAULT_ROLE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_ORVSDMLA56612EAEFIQ6WL5OI` (`NAME`),
  KEY `IDX_REALM_MASTER_ADM_CLI` (`MASTER_ADMIN_CLIENT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM`
--

/*!40000 ALTER TABLE `REALM` DISABLE KEYS */;
INSERT INTO `REALM` VALUES ('736d3e5b-4c46-41ed-9afb-47e727ecab9e',60,300,60,NULL,NULL,NULL,1,0,0,NULL,'master',0,NULL,0,0,0,0,'EXTERNAL',1800,36000,0,0,'3fc95085-aba5-4a9e-95d5-532e3afad9f2',1800,0,NULL,0,0,0,0,0,1,30,6,'HmacSHA1','totp','4e3a7162-5fe3-42a5-9ddb-3f1326baa7e9','14fc0faa-47cb-4e83-b11a-e5dd766fe5ce','7a195384-cbf6-4e7d-9c6d-7a3008aeeee0','701651b2-d532-4922-8afa-6d93114d85b4','bc21a24a-d001-4d96-aaf8-062112ed1e2b',2592000,0,900,1,0,'dc2bb502-e5df-4e86-b135-7810a3780ab2',0,0,0,0,'e61e14ab-a1b6-47e3-a182-0c80ce0cc23b'),('b90e8c11-9258-44ee-901a-1650b6e14601',60,300,300,NULL,NULL,NULL,1,1,259200,NULL,'keycloak',0,NULL,0,0,0,0,'EXTERNAL',1800,36000,0,0,'36d6476a-889e-4395-bf12-9fc79c217c36',1800,0,NULL,0,1,0,0,0,1,30,6,'HmacSHA1','totp','919e97ab-8fa3-4795-b38b-cef08e92bd01','052cc613-6116-46a5-b2cd-a8d5f1b8eeb0','c75b3bc1-3d7e-4e5c-85bb-0988ffae04e8','e676ea9b-7621-4bed-b24a-42a5fe955f11','de078332-f55f-4944-9bf4-b6ca668f56ab',2592000,0,900,1,0,'67700e74-8e4b-4caf-80b5-8b3bdb8a6b0b',0,0,0,0,'60dbc25e-b0fa-4335-8251-cef1e45d2465');
/*!40000 ALTER TABLE `REALM` ENABLE KEYS */;

--
-- Table structure for table `REALM_ATTRIBUTE`
--

DROP TABLE IF EXISTS `REALM_ATTRIBUTE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_ATTRIBUTE` (
  `NAME` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  `VALUE` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  PRIMARY KEY (`NAME`,`REALM_ID`),
  KEY `IDX_REALM_ATTR_REALM` (`REALM_ID`),
  CONSTRAINT `FK_8SHXD6L3E9ATQUKACXGPFFPTW` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_ATTRIBUTE`
--

/*!40000 ALTER TABLE `REALM_ATTRIBUTE` DISABLE KEYS */;
INSERT INTO `REALM_ATTRIBUTE` VALUES ('_browser_header.contentSecurityPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','frame-src \'self\'; frame-ancestors \'self\'; object-src \'none\';'),('_browser_header.contentSecurityPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','frame-src \'self\'; frame-ancestors \'self\'; object-src \'none\';'),('_browser_header.contentSecurityPolicyReportOnly','736d3e5b-4c46-41ed-9afb-47e727ecab9e',''),('_browser_header.contentSecurityPolicyReportOnly','b90e8c11-9258-44ee-901a-1650b6e14601',''),('_browser_header.referrerPolicy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','no-referrer'),('_browser_header.referrerPolicy','b90e8c11-9258-44ee-901a-1650b6e14601','no-referrer'),('_browser_header.strictTransportSecurity','736d3e5b-4c46-41ed-9afb-47e727ecab9e','max-age=31536000; includeSubDomains'),('_browser_header.strictTransportSecurity','b90e8c11-9258-44ee-901a-1650b6e14601','max-age=31536000; includeSubDomains'),('_browser_header.xContentTypeOptions','736d3e5b-4c46-41ed-9afb-47e727ecab9e','nosniff'),('_browser_header.xContentTypeOptions','b90e8c11-9258-44ee-901a-1650b6e14601','nosniff'),('_browser_header.xFrameOptions','736d3e5b-4c46-41ed-9afb-47e727ecab9e','SAMEORIGIN'),('_browser_header.xFrameOptions','b90e8c11-9258-44ee-901a-1650b6e14601','SAMEORIGIN'),('_browser_header.xRobotsTag','736d3e5b-4c46-41ed-9afb-47e727ecab9e','none'),('_browser_header.xRobotsTag','b90e8c11-9258-44ee-901a-1650b6e14601','none'),('actionTokenGeneratedByAdminLifespan','b90e8c11-9258-44ee-901a-1650b6e14601','43200'),('actionTokenGeneratedByUserLifespan','b90e8c11-9258-44ee-901a-1650b6e14601','300'),('adminEventsExpiration','b90e8c11-9258-44ee-901a-1650b6e14601','259200'),('adminPermissionsEnabled','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('bruteForceProtected','736d3e5b-4c46-41ed-9afb-47e727ecab9e','false'),('bruteForceProtected','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('bruteForceStrategy','736d3e5b-4c46-41ed-9afb-47e727ecab9e','MULTIPLE'),('bruteForceStrategy','b90e8c11-9258-44ee-901a-1650b6e14601','MULTIPLE'),('cibaAuthRequestedUserHint','b90e8c11-9258-44ee-901a-1650b6e14601','login_hint'),('cibaBackchannelTokenDeliveryMode','b90e8c11-9258-44ee-901a-1650b6e14601','poll'),('cibaExpiresIn','b90e8c11-9258-44ee-901a-1650b6e14601','120'),('cibaInterval','b90e8c11-9258-44ee-901a-1650b6e14601','5'),('client-policies.policies','b90e8c11-9258-44ee-901a-1650b6e14601','{\"policies\":[]}'),('client-policies.profiles','b90e8c11-9258-44ee-901a-1650b6e14601','{\"profiles\":[]}'),('clientOfflineSessionIdleTimeout','b90e8c11-9258-44ee-901a-1650b6e14601','0'),('clientOfflineSessionMaxLifespan','b90e8c11-9258-44ee-901a-1650b6e14601','0'),('clientSessionIdleTimeout','b90e8c11-9258-44ee-901a-1650b6e14601','0'),('clientSessionMaxLifespan','b90e8c11-9258-44ee-901a-1650b6e14601','0'),('defaultSignatureAlgorithm','736d3e5b-4c46-41ed-9afb-47e727ecab9e','RS256'),('defaultSignatureAlgorithm','b90e8c11-9258-44ee-901a-1650b6e14601','RS256'),('displayName','736d3e5b-4c46-41ed-9afb-47e727ecab9e','Keycloak'),('displayNameHtml','736d3e5b-4c46-41ed-9afb-47e727ecab9e','<div class=\"kc-logo-text\"><span>Keycloak</span></div>'),('failureFactor','736d3e5b-4c46-41ed-9afb-47e727ecab9e','30'),('failureFactor','b90e8c11-9258-44ee-901a-1650b6e14601','30'),('firstBrokerLoginFlowId','736d3e5b-4c46-41ed-9afb-47e727ecab9e','612c3b47-8df0-45e8-bb22-42ef53e57d3c'),('firstBrokerLoginFlowId','b90e8c11-9258-44ee-901a-1650b6e14601','9bc9ebf8-8728-40df-92f4-baa7e060d6b6'),('maxDeltaTimeSeconds','736d3e5b-4c46-41ed-9afb-47e727ecab9e','43200'),('maxDeltaTimeSeconds','b90e8c11-9258-44ee-901a-1650b6e14601','43200'),('maxFailureWaitSeconds','736d3e5b-4c46-41ed-9afb-47e727ecab9e','900'),('maxFailureWaitSeconds','b90e8c11-9258-44ee-901a-1650b6e14601','900'),('maxTemporaryLockouts','736d3e5b-4c46-41ed-9afb-47e727ecab9e','0'),('maxTemporaryLockouts','b90e8c11-9258-44ee-901a-1650b6e14601','0'),('minimumQuickLoginWaitSeconds','736d3e5b-4c46-41ed-9afb-47e727ecab9e','60'),('minimumQuickLoginWaitSeconds','b90e8c11-9258-44ee-901a-1650b6e14601','60'),('oauth2DeviceCodeLifespan','b90e8c11-9258-44ee-901a-1650b6e14601','600'),('oauth2DevicePollingInterval','b90e8c11-9258-44ee-901a-1650b6e14601','5'),('offlineSessionMaxLifespan','736d3e5b-4c46-41ed-9afb-47e727ecab9e','5184000'),('offlineSessionMaxLifespan','b90e8c11-9258-44ee-901a-1650b6e14601','5184000'),('offlineSessionMaxLifespanEnabled','736d3e5b-4c46-41ed-9afb-47e727ecab9e','false'),('offlineSessionMaxLifespanEnabled','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('organizationsEnabled','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('parRequestUriLifespan','b90e8c11-9258-44ee-901a-1650b6e14601','60'),('permanentLockout','736d3e5b-4c46-41ed-9afb-47e727ecab9e','false'),('permanentLockout','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('quickLoginCheckMilliSeconds','736d3e5b-4c46-41ed-9afb-47e727ecab9e','1000'),('quickLoginCheckMilliSeconds','b90e8c11-9258-44ee-901a-1650b6e14601','1000'),('realmReusableOtpCode','736d3e5b-4c46-41ed-9afb-47e727ecab9e','false'),('realmReusableOtpCode','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('verifiableCredentialsEnabled','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('waitIncrementSeconds','736d3e5b-4c46-41ed-9afb-47e727ecab9e','60'),('waitIncrementSeconds','b90e8c11-9258-44ee-901a-1650b6e14601','60'),('webAuthnPolicyAttestationConveyancePreference','b90e8c11-9258-44ee-901a-1650b6e14601','not specified'),('webAuthnPolicyAttestationConveyancePreferencePasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','not specified'),('webAuthnPolicyAuthenticatorAttachment','b90e8c11-9258-44ee-901a-1650b6e14601','not specified'),('webAuthnPolicyAuthenticatorAttachmentPasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','not specified'),('webAuthnPolicyAvoidSameAuthenticatorRegister','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('webAuthnPolicyAvoidSameAuthenticatorRegisterPasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','false'),('webAuthnPolicyCreateTimeout','b90e8c11-9258-44ee-901a-1650b6e14601','0'),('webAuthnPolicyCreateTimeoutPasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','0'),('webAuthnPolicyRequireResidentKey','b90e8c11-9258-44ee-901a-1650b6e14601','not specified'),('webAuthnPolicyRequireResidentKeyPasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','not specified'),('webAuthnPolicyRpEntityName','b90e8c11-9258-44ee-901a-1650b6e14601','keycloak'),('webAuthnPolicyRpEntityNamePasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','keycloak'),('webAuthnPolicyRpId','b90e8c11-9258-44ee-901a-1650b6e14601',''),('webAuthnPolicyRpIdPasswordless','b90e8c11-9258-44ee-901a-1650b6e14601',''),('webAuthnPolicySignatureAlgorithms','b90e8c11-9258-44ee-901a-1650b6e14601','ES256,RS256'),('webAuthnPolicySignatureAlgorithmsPasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','ES256,RS256'),('webAuthnPolicyUserVerificationRequirement','b90e8c11-9258-44ee-901a-1650b6e14601','not specified'),('webAuthnPolicyUserVerificationRequirementPasswordless','b90e8c11-9258-44ee-901a-1650b6e14601','not specified');
/*!40000 ALTER TABLE `REALM_ATTRIBUTE` ENABLE KEYS */;

--
-- Table structure for table `REALM_DEFAULT_GROUPS`
--

DROP TABLE IF EXISTS `REALM_DEFAULT_GROUPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_DEFAULT_GROUPS` (
  `REALM_ID` varchar(36) NOT NULL,
  `GROUP_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`REALM_ID`,`GROUP_ID`),
  UNIQUE KEY `CON_GROUP_ID_DEF_GROUPS` (`GROUP_ID`),
  KEY `IDX_REALM_DEF_GRP_REALM` (`REALM_ID`),
  CONSTRAINT `FK_DEF_GROUPS_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_DEFAULT_GROUPS`
--

/*!40000 ALTER TABLE `REALM_DEFAULT_GROUPS` DISABLE KEYS */;
/*!40000 ALTER TABLE `REALM_DEFAULT_GROUPS` ENABLE KEYS */;

--
-- Table structure for table `REALM_ENABLED_EVENT_TYPES`
--

DROP TABLE IF EXISTS `REALM_ENABLED_EVENT_TYPES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_ENABLED_EVENT_TYPES` (
  `REALM_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  PRIMARY KEY (`REALM_ID`,`VALUE`),
  KEY `IDX_REALM_EVT_TYPES_REALM` (`REALM_ID`),
  CONSTRAINT `FK_H846O4H0W8EPX5NWEDRF5Y69J` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_ENABLED_EVENT_TYPES`
--

/*!40000 ALTER TABLE `REALM_ENABLED_EVENT_TYPES` DISABLE KEYS */;
INSERT INTO `REALM_ENABLED_EVENT_TYPES` VALUES ('b90e8c11-9258-44ee-901a-1650b6e14601','AUTHREQID_TO_TOKEN'),('b90e8c11-9258-44ee-901a-1650b6e14601','AUTHREQID_TO_TOKEN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_DELETE'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_DELETE_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_INITIATED_ACCOUNT_LINKING'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_INITIATED_ACCOUNT_LINKING_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_LOGIN'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_LOGIN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_REGISTER'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_REGISTER_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_UPDATE'),('b90e8c11-9258-44ee-901a-1650b6e14601','CLIENT_UPDATE_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','CODE_TO_TOKEN'),('b90e8c11-9258-44ee-901a-1650b6e14601','CODE_TO_TOKEN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','CUSTOM_REQUIRED_ACTION'),('b90e8c11-9258-44ee-901a-1650b6e14601','CUSTOM_REQUIRED_ACTION_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','DELETE_ACCOUNT'),('b90e8c11-9258-44ee-901a-1650b6e14601','DELETE_ACCOUNT_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','EXECUTE_ACTION_TOKEN'),('b90e8c11-9258-44ee-901a-1650b6e14601','EXECUTE_ACTION_TOKEN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','EXECUTE_ACTIONS'),('b90e8c11-9258-44ee-901a-1650b6e14601','EXECUTE_ACTIONS_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','FEDERATED_IDENTITY_LINK'),('b90e8c11-9258-44ee-901a-1650b6e14601','FEDERATED_IDENTITY_LINK_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','FEDERATED_IDENTITY_OVERRIDE_LINK'),('b90e8c11-9258-44ee-901a-1650b6e14601','FEDERATED_IDENTITY_OVERRIDE_LINK_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','GRANT_CONSENT'),('b90e8c11-9258-44ee-901a-1650b6e14601','GRANT_CONSENT_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','IDENTITY_PROVIDER_FIRST_LOGIN'),('b90e8c11-9258-44ee-901a-1650b6e14601','IDENTITY_PROVIDER_FIRST_LOGIN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','IDENTITY_PROVIDER_LINK_ACCOUNT'),('b90e8c11-9258-44ee-901a-1650b6e14601','IDENTITY_PROVIDER_LINK_ACCOUNT_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','IDENTITY_PROVIDER_POST_LOGIN'),('b90e8c11-9258-44ee-901a-1650b6e14601','IDENTITY_PROVIDER_POST_LOGIN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','IMPERSONATE'),('b90e8c11-9258-44ee-901a-1650b6e14601','IMPERSONATE_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','INVITE_ORG'),('b90e8c11-9258-44ee-901a-1650b6e14601','INVITE_ORG_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','LOGIN'),('b90e8c11-9258-44ee-901a-1650b6e14601','LOGIN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','LOGOUT'),('b90e8c11-9258-44ee-901a-1650b6e14601','LOGOUT_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_DEVICE_AUTH'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_DEVICE_AUTH_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_DEVICE_CODE_TO_TOKEN'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_DEVICE_CODE_TO_TOKEN_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_DEVICE_VERIFY_USER_CODE'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_DEVICE_VERIFY_USER_CODE_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_EXTENSION_GRANT'),('b90e8c11-9258-44ee-901a-1650b6e14601','OAUTH2_EXTENSION_GRANT_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','PERMISSION_TOKEN'),('b90e8c11-9258-44ee-901a-1650b6e14601','REGISTER'),('b90e8c11-9258-44ee-901a-1650b6e14601','REGISTER_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','REMOVE_CREDENTIAL'),('b90e8c11-9258-44ee-901a-1650b6e14601','REMOVE_CREDENTIAL_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','REMOVE_FEDERATED_IDENTITY'),('b90e8c11-9258-44ee-901a-1650b6e14601','REMOVE_FEDERATED_IDENTITY_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','REMOVE_TOTP'),('b90e8c11-9258-44ee-901a-1650b6e14601','REMOVE_TOTP_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','RESET_PASSWORD'),('b90e8c11-9258-44ee-901a-1650b6e14601','RESET_PASSWORD_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','RESTART_AUTHENTICATION'),('b90e8c11-9258-44ee-901a-1650b6e14601','RESTART_AUTHENTICATION_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','REVOKE_GRANT'),('b90e8c11-9258-44ee-901a-1650b6e14601','REVOKE_GRANT_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','SEND_IDENTITY_PROVIDER_LINK'),('b90e8c11-9258-44ee-901a-1650b6e14601','SEND_IDENTITY_PROVIDER_LINK_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','SEND_RESET_PASSWORD'),('b90e8c11-9258-44ee-901a-1650b6e14601','SEND_RESET_PASSWORD_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','SEND_VERIFY_EMAIL'),('b90e8c11-9258-44ee-901a-1650b6e14601','SEND_VERIFY_EMAIL_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','TOKEN_EXCHANGE'),('b90e8c11-9258-44ee-901a-1650b6e14601','TOKEN_EXCHANGE_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_CONSENT'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_CONSENT_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_CREDENTIAL'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_CREDENTIAL_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_EMAIL'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_EMAIL_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_PASSWORD'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_PASSWORD_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_PROFILE'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_PROFILE_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_TOTP'),('b90e8c11-9258-44ee-901a-1650b6e14601','UPDATE_TOTP_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','USER_DISABLED_BY_PERMANENT_LOCKOUT'),('b90e8c11-9258-44ee-901a-1650b6e14601','USER_DISABLED_BY_TEMPORARY_LOCKOUT'),('b90e8c11-9258-44ee-901a-1650b6e14601','VERIFY_EMAIL'),('b90e8c11-9258-44ee-901a-1650b6e14601','VERIFY_EMAIL_ERROR'),('b90e8c11-9258-44ee-901a-1650b6e14601','VERIFY_PROFILE'),('b90e8c11-9258-44ee-901a-1650b6e14601','VERIFY_PROFILE_ERROR');
/*!40000 ALTER TABLE `REALM_ENABLED_EVENT_TYPES` ENABLE KEYS */;

--
-- Table structure for table `REALM_EVENTS_LISTENERS`
--

DROP TABLE IF EXISTS `REALM_EVENTS_LISTENERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_EVENTS_LISTENERS` (
  `REALM_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  PRIMARY KEY (`REALM_ID`,`VALUE`),
  KEY `IDX_REALM_EVT_LIST_REALM` (`REALM_ID`),
  CONSTRAINT `FK_H846O4H0W8EPX5NXEV9F5Y69J` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_EVENTS_LISTENERS`
--

/*!40000 ALTER TABLE `REALM_EVENTS_LISTENERS` DISABLE KEYS */;
INSERT INTO `REALM_EVENTS_LISTENERS` VALUES ('736d3e5b-4c46-41ed-9afb-47e727ecab9e','jboss-logging'),('b90e8c11-9258-44ee-901a-1650b6e14601','jboss-logging');
/*!40000 ALTER TABLE `REALM_EVENTS_LISTENERS` ENABLE KEYS */;

--
-- Table structure for table `REALM_LOCALIZATIONS`
--

DROP TABLE IF EXISTS `REALM_LOCALIZATIONS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_LOCALIZATIONS` (
  `REALM_ID` varchar(255) NOT NULL,
  `LOCALE` varchar(255) NOT NULL,
  `TEXTS` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  PRIMARY KEY (`REALM_ID`,`LOCALE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_LOCALIZATIONS`
--

/*!40000 ALTER TABLE `REALM_LOCALIZATIONS` DISABLE KEYS */;
/*!40000 ALTER TABLE `REALM_LOCALIZATIONS` ENABLE KEYS */;

--
-- Table structure for table `REALM_REQUIRED_CREDENTIAL`
--

DROP TABLE IF EXISTS `REALM_REQUIRED_CREDENTIAL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_REQUIRED_CREDENTIAL` (
  `TYPE` varchar(255) NOT NULL,
  `FORM_LABEL` varchar(255) DEFAULT NULL,
  `INPUT` tinyint NOT NULL DEFAULT '0',
  `SECRET` tinyint NOT NULL DEFAULT '0',
  `REALM_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`REALM_ID`,`TYPE`),
  CONSTRAINT `FK_5HG65LYBEVAVKQFKI3KPONH9V` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_REQUIRED_CREDENTIAL`
--

/*!40000 ALTER TABLE `REALM_REQUIRED_CREDENTIAL` DISABLE KEYS */;
INSERT INTO `REALM_REQUIRED_CREDENTIAL` VALUES ('password','password',1,1,'736d3e5b-4c46-41ed-9afb-47e727ecab9e'),('password','password',1,1,'b90e8c11-9258-44ee-901a-1650b6e14601');
/*!40000 ALTER TABLE `REALM_REQUIRED_CREDENTIAL` ENABLE KEYS */;

--
-- Table structure for table `REALM_SMTP_CONFIG`
--

DROP TABLE IF EXISTS `REALM_SMTP_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_SMTP_CONFIG` (
  `REALM_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`REALM_ID`,`NAME`),
  CONSTRAINT `FK_70EJ8XDXGXD0B9HH6180IRR0O` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_SMTP_CONFIG`
--

/*!40000 ALTER TABLE `REALM_SMTP_CONFIG` DISABLE KEYS */;
/*!40000 ALTER TABLE `REALM_SMTP_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `REALM_SUPPORTED_LOCALES`
--

DROP TABLE IF EXISTS `REALM_SUPPORTED_LOCALES`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REALM_SUPPORTED_LOCALES` (
  `REALM_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  PRIMARY KEY (`REALM_ID`,`VALUE`),
  KEY `IDX_REALM_SUPP_LOCAL_REALM` (`REALM_ID`),
  CONSTRAINT `FK_SUPPORTED_LOCALES_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REALM_SUPPORTED_LOCALES`
--

/*!40000 ALTER TABLE `REALM_SUPPORTED_LOCALES` DISABLE KEYS */;
/*!40000 ALTER TABLE `REALM_SUPPORTED_LOCALES` ENABLE KEYS */;

--
-- Table structure for table `REDIRECT_URIS`
--

DROP TABLE IF EXISTS `REDIRECT_URIS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REDIRECT_URIS` (
  `CLIENT_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  PRIMARY KEY (`CLIENT_ID`,`VALUE`),
  KEY `IDX_REDIR_URI_CLIENT` (`CLIENT_ID`),
  CONSTRAINT `FK_1BURS8PB4OUJ97H5WUPPAHV9F` FOREIGN KEY (`CLIENT_ID`) REFERENCES `CLIENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REDIRECT_URIS`
--

/*!40000 ALTER TABLE `REDIRECT_URIS` DISABLE KEYS */;
INSERT INTO `REDIRECT_URIS` VALUES ('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','*'),('5899f04c-e3bf-467b-bf8d-ad2b5bc031fa','/realms/keycloak/account/*'),('703aca18-8a9e-44ea-bf88-648ae965c214','/realms/master/account/*'),('70ac515d-37ca-4129-9a1f-9aceab32bcd0','/realms/master/account/*'),('7793a774-b465-4b0b-97d1-08840aa08c9a','/admin/keycloak/console/*'),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','/realms/keycloak/account/*'),('e12fceb4-c28d-4e75-8f26-48fe7824c110','/admin/master/console/*');
/*!40000 ALTER TABLE `REDIRECT_URIS` ENABLE KEYS */;

--
-- Table structure for table `REQUIRED_ACTION_CONFIG`
--

DROP TABLE IF EXISTS `REQUIRED_ACTION_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REQUIRED_ACTION_CONFIG` (
  `REQUIRED_ACTION_ID` varchar(36) NOT NULL,
  `VALUE` longtext,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`REQUIRED_ACTION_ID`,`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REQUIRED_ACTION_CONFIG`
--

/*!40000 ALTER TABLE `REQUIRED_ACTION_CONFIG` DISABLE KEYS */;
/*!40000 ALTER TABLE `REQUIRED_ACTION_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `REQUIRED_ACTION_PROVIDER`
--

DROP TABLE IF EXISTS `REQUIRED_ACTION_PROVIDER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REQUIRED_ACTION_PROVIDER` (
  `ID` varchar(36) NOT NULL,
  `ALIAS` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  `ENABLED` tinyint NOT NULL DEFAULT '0',
  `DEFAULT_ACTION` tinyint NOT NULL DEFAULT '0',
  `PROVIDER_ID` varchar(255) DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_REQ_ACT_PROV_REALM` (`REALM_ID`),
  CONSTRAINT `FK_REQ_ACT_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REQUIRED_ACTION_PROVIDER`
--

/*!40000 ALTER TABLE `REQUIRED_ACTION_PROVIDER` DISABLE KEYS */;
INSERT INTO `REQUIRED_ACTION_PROVIDER` VALUES ('0b0e1e70-174c-46ac-a529-fd07ef3f444f','delete_credential','Delete Credential','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'delete_credential',100),('2cf344ae-55fb-4b6c-b1e9-a43eb73b73dd','VERIFY_EMAIL','Verify Email','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'VERIFY_EMAIL',50),('3ffcee0d-794e-4dee-b3f7-01700594a5a8','update_user_locale','Update User Locale','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'update_user_locale',1000),('45993baa-d9a0-4f3e-b8ce-1c7e70b493f4','webauthn-register-passwordless','Webauthn Register Passwordless','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'webauthn-register-passwordless',80),('4b5097c8-d539-40a4-ae0c-9dc79ae4958e','VERIFY_EMAIL','Verify Email','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'VERIFY_EMAIL',50),('51c8d24e-0134-4f54-a360-0ab39566770d','CONFIGURE_TOTP','Configure OTP','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'CONFIGURE_TOTP',10),('571d7af9-cecb-4855-8f0a-d7bee4020da7','delete_account','Delete Account','b90e8c11-9258-44ee-901a-1650b6e14601',0,0,'delete_account',60),('5d02eb46-b268-427a-83a0-7f30f24c6fa1','update_user_locale','Update User Locale','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'update_user_locale',1000),('64f6923e-83c4-41ab-8774-6caa896a0ed5','webauthn-register-passwordless','Webauthn Register Passwordless','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'webauthn-register-passwordless',80),('73be2d16-f6e8-4e2a-b1f6-70665ac424ed','VERIFY_PROFILE','Verify Profile','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'VERIFY_PROFILE',90),('7e724ae1-03d4-40ce-a9a4-b77b8afa6bde','delete_credential','Delete Credential','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'delete_credential',100),('815cddbb-d1e9-451f-9d5d-df13e2a5632d','CONFIGURE_TOTP','Configure OTP','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'CONFIGURE_TOTP',10),('82c10c1a-1c20-4659-b589-049c5f24720d','UPDATE_PROFILE','Update Profile','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'UPDATE_PROFILE',40),('87001da8-2d1a-4a5e-8547-0690fd99709d','webauthn-register','Webauthn Register','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'webauthn-register',70),('94ebff24-934e-47bb-836a-aae927163170','UPDATE_PASSWORD','Update Password','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'UPDATE_PASSWORD',30),('9b59986a-6514-4edb-a4db-446952df1e1e','UPDATE_PROFILE','Update Profile','b90e8c11-9258-44ee-901a-1650b6e14601',1,0,'UPDATE_PROFILE',40),('a129eb12-39c9-4235-8a07-afec2e257cf9','delete_account','Delete Account','736d3e5b-4c46-41ed-9afb-47e727ecab9e',0,0,'delete_account',60),('a6ac3a92-4982-48bf-9400-ffc8dfd063fd','VERIFY_PROFILE','Verify Profile','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'VERIFY_PROFILE',90),('dfc963dc-f6f5-42d8-b74b-7e100b465fe8','webauthn-register','Webauthn Register','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'webauthn-register',70),('e103ceb1-dc29-46d3-97e4-79c0dfd5d10b','TERMS_AND_CONDITIONS','Terms and Conditions','736d3e5b-4c46-41ed-9afb-47e727ecab9e',0,0,'TERMS_AND_CONDITIONS',20),('f74fffaf-5d60-4ddd-b888-acc41f692ccf','TERMS_AND_CONDITIONS','Terms and Conditions','b90e8c11-9258-44ee-901a-1650b6e14601',0,0,'TERMS_AND_CONDITIONS',20),('fbcb4c37-b16a-499f-a457-2c40f42e8c55','UPDATE_PASSWORD','Update Password','736d3e5b-4c46-41ed-9afb-47e727ecab9e',1,0,'UPDATE_PASSWORD',30);
/*!40000 ALTER TABLE `REQUIRED_ACTION_PROVIDER` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_ATTRIBUTE`
--

DROP TABLE IF EXISTS `RESOURCE_ATTRIBUTE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_ATTRIBUTE` (
  `ID` varchar(36) NOT NULL DEFAULT 'sybase-needs-something-here',
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `RESOURCE_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_5HRM2VLF9QL5FU022KQEPOVBR` (`RESOURCE_ID`),
  CONSTRAINT `FK_5HRM2VLF9QL5FU022KQEPOVBR` FOREIGN KEY (`RESOURCE_ID`) REFERENCES `RESOURCE_SERVER_RESOURCE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_ATTRIBUTE`
--

/*!40000 ALTER TABLE `RESOURCE_ATTRIBUTE` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_ATTRIBUTE` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_POLICY`
--

DROP TABLE IF EXISTS `RESOURCE_POLICY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_POLICY` (
  `RESOURCE_ID` varchar(36) NOT NULL,
  `POLICY_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`RESOURCE_ID`,`POLICY_ID`),
  KEY `IDX_RES_POLICY_POLICY` (`POLICY_ID`),
  CONSTRAINT `FK_FRSRPOS53XCX4WNKOG82SSRFY` FOREIGN KEY (`RESOURCE_ID`) REFERENCES `RESOURCE_SERVER_RESOURCE` (`ID`),
  CONSTRAINT `FK_FRSRPP213XCX4WNKOG82SSRFY` FOREIGN KEY (`POLICY_ID`) REFERENCES `RESOURCE_SERVER_POLICY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_POLICY`
--

/*!40000 ALTER TABLE `RESOURCE_POLICY` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_POLICY` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_SCOPE`
--

DROP TABLE IF EXISTS `RESOURCE_SCOPE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_SCOPE` (
  `RESOURCE_ID` varchar(36) NOT NULL,
  `SCOPE_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`RESOURCE_ID`,`SCOPE_ID`),
  KEY `IDX_RES_SCOPE_SCOPE` (`SCOPE_ID`),
  CONSTRAINT `FK_FRSRPOS13XCX4WNKOG82SSRFY` FOREIGN KEY (`RESOURCE_ID`) REFERENCES `RESOURCE_SERVER_RESOURCE` (`ID`),
  CONSTRAINT `FK_FRSRPS213XCX4WNKOG82SSRFY` FOREIGN KEY (`SCOPE_ID`) REFERENCES `RESOURCE_SERVER_SCOPE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_SCOPE`
--

/*!40000 ALTER TABLE `RESOURCE_SCOPE` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_SCOPE` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_SERVER`
--

DROP TABLE IF EXISTS `RESOURCE_SERVER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_SERVER` (
  `ID` varchar(36) NOT NULL,
  `ALLOW_RS_REMOTE_MGMT` tinyint NOT NULL DEFAULT '0',
  `POLICY_ENFORCE_MODE` tinyint DEFAULT NULL,
  `DECISION_STRATEGY` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_SERVER`
--

/*!40000 ALTER TABLE `RESOURCE_SERVER` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_SERVER` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_SERVER_PERM_TICKET`
--

DROP TABLE IF EXISTS `RESOURCE_SERVER_PERM_TICKET`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_SERVER_PERM_TICKET` (
  `ID` varchar(36) NOT NULL,
  `OWNER` varchar(255) DEFAULT NULL,
  `REQUESTER` varchar(255) DEFAULT NULL,
  `CREATED_TIMESTAMP` bigint NOT NULL,
  `GRANTED_TIMESTAMP` bigint DEFAULT NULL,
  `RESOURCE_ID` varchar(36) NOT NULL,
  `SCOPE_ID` varchar(36) DEFAULT NULL,
  `RESOURCE_SERVER_ID` varchar(36) NOT NULL,
  `POLICY_ID` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_FRSR6T700S9V50BU18WS5PMT` (`OWNER`,`REQUESTER`,`RESOURCE_SERVER_ID`,`RESOURCE_ID`,`SCOPE_ID`),
  KEY `FK_FRSRHO213XCX4WNKOG82SSPMT` (`RESOURCE_SERVER_ID`),
  KEY `FK_FRSRHO213XCX4WNKOG83SSPMT` (`RESOURCE_ID`),
  KEY `FK_FRSRHO213XCX4WNKOG84SSPMT` (`SCOPE_ID`),
  KEY `FK_FRSRPO2128CX4WNKOG82SSRFY` (`POLICY_ID`),
  KEY `IDX_PERM_TICKET_REQUESTER` (`REQUESTER`),
  KEY `IDX_PERM_TICKET_OWNER` (`OWNER`),
  CONSTRAINT `FK_FRSRHO213XCX4WNKOG82SSPMT` FOREIGN KEY (`RESOURCE_SERVER_ID`) REFERENCES `RESOURCE_SERVER` (`ID`),
  CONSTRAINT `FK_FRSRHO213XCX4WNKOG83SSPMT` FOREIGN KEY (`RESOURCE_ID`) REFERENCES `RESOURCE_SERVER_RESOURCE` (`ID`),
  CONSTRAINT `FK_FRSRHO213XCX4WNKOG84SSPMT` FOREIGN KEY (`SCOPE_ID`) REFERENCES `RESOURCE_SERVER_SCOPE` (`ID`),
  CONSTRAINT `FK_FRSRPO2128CX4WNKOG82SSRFY` FOREIGN KEY (`POLICY_ID`) REFERENCES `RESOURCE_SERVER_POLICY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_SERVER_PERM_TICKET`
--

/*!40000 ALTER TABLE `RESOURCE_SERVER_PERM_TICKET` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_SERVER_PERM_TICKET` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_SERVER_POLICY`
--

DROP TABLE IF EXISTS `RESOURCE_SERVER_POLICY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_SERVER_POLICY` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `TYPE` varchar(255) NOT NULL,
  `DECISION_STRATEGY` tinyint DEFAULT NULL,
  `LOGIC` tinyint DEFAULT NULL,
  `RESOURCE_SERVER_ID` varchar(36) DEFAULT NULL,
  `OWNER` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_FRSRPT700S9V50BU18WS5HA6` (`NAME`,`RESOURCE_SERVER_ID`),
  KEY `IDX_RES_SERV_POL_RES_SERV` (`RESOURCE_SERVER_ID`),
  CONSTRAINT `FK_FRSRPO213XCX4WNKOG82SSRFY` FOREIGN KEY (`RESOURCE_SERVER_ID`) REFERENCES `RESOURCE_SERVER` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_SERVER_POLICY`
--

/*!40000 ALTER TABLE `RESOURCE_SERVER_POLICY` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_SERVER_POLICY` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_SERVER_RESOURCE`
--

DROP TABLE IF EXISTS `RESOURCE_SERVER_RESOURCE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_SERVER_RESOURCE` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `TYPE` varchar(255) DEFAULT NULL,
  `ICON_URI` varchar(255) DEFAULT NULL,
  `OWNER` varchar(255) DEFAULT NULL,
  `RESOURCE_SERVER_ID` varchar(36) DEFAULT NULL,
  `OWNER_MANAGED_ACCESS` tinyint NOT NULL DEFAULT '0',
  `DISPLAY_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_FRSR6T700S9V50BU18WS5HA6` (`NAME`,`OWNER`,`RESOURCE_SERVER_ID`),
  KEY `IDX_RES_SRV_RES_RES_SRV` (`RESOURCE_SERVER_ID`),
  CONSTRAINT `FK_FRSRHO213XCX4WNKOG82SSRFY` FOREIGN KEY (`RESOURCE_SERVER_ID`) REFERENCES `RESOURCE_SERVER` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_SERVER_RESOURCE`
--

/*!40000 ALTER TABLE `RESOURCE_SERVER_RESOURCE` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_SERVER_RESOURCE` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_SERVER_SCOPE`
--

DROP TABLE IF EXISTS `RESOURCE_SERVER_SCOPE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_SERVER_SCOPE` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `ICON_URI` varchar(255) DEFAULT NULL,
  `RESOURCE_SERVER_ID` varchar(36) DEFAULT NULL,
  `DISPLAY_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_FRSRST700S9V50BU18WS5HA6` (`NAME`,`RESOURCE_SERVER_ID`),
  KEY `IDX_RES_SRV_SCOPE_RES_SRV` (`RESOURCE_SERVER_ID`),
  CONSTRAINT `FK_FRSRSO213XCX4WNKOG82SSRFY` FOREIGN KEY (`RESOURCE_SERVER_ID`) REFERENCES `RESOURCE_SERVER` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_SERVER_SCOPE`
--

/*!40000 ALTER TABLE `RESOURCE_SERVER_SCOPE` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_SERVER_SCOPE` ENABLE KEYS */;

--
-- Table structure for table `RESOURCE_URIS`
--

DROP TABLE IF EXISTS `RESOURCE_URIS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `RESOURCE_URIS` (
  `RESOURCE_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  PRIMARY KEY (`RESOURCE_ID`,`VALUE`),
  CONSTRAINT `FK_RESOURCE_SERVER_URIS` FOREIGN KEY (`RESOURCE_ID`) REFERENCES `RESOURCE_SERVER_RESOURCE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `RESOURCE_URIS`
--

/*!40000 ALTER TABLE `RESOURCE_URIS` DISABLE KEYS */;
/*!40000 ALTER TABLE `RESOURCE_URIS` ENABLE KEYS */;

--
-- Table structure for table `REVOKED_TOKEN`
--

DROP TABLE IF EXISTS `REVOKED_TOKEN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `REVOKED_TOKEN` (
  `ID` varchar(255) NOT NULL,
  `EXPIRE` bigint NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_REV_TOKEN_ON_EXPIRE` (`EXPIRE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `REVOKED_TOKEN`
--

/*!40000 ALTER TABLE `REVOKED_TOKEN` DISABLE KEYS */;
/*!40000 ALTER TABLE `REVOKED_TOKEN` ENABLE KEYS */;

--
-- Table structure for table `ROLE_ATTRIBUTE`
--

DROP TABLE IF EXISTS `ROLE_ATTRIBUTE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ROLE_ATTRIBUTE` (
  `ID` varchar(36) NOT NULL,
  `ROLE_ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_ROLE_ATTRIBUTE` (`ROLE_ID`),
  CONSTRAINT `FK_ROLE_ATTRIBUTE_ID` FOREIGN KEY (`ROLE_ID`) REFERENCES `KEYCLOAK_ROLE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ROLE_ATTRIBUTE`
--

/*!40000 ALTER TABLE `ROLE_ATTRIBUTE` DISABLE KEYS */;
/*!40000 ALTER TABLE `ROLE_ATTRIBUTE` ENABLE KEYS */;

--
-- Table structure for table `SCOPE_MAPPING`
--

DROP TABLE IF EXISTS `SCOPE_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SCOPE_MAPPING` (
  `CLIENT_ID` varchar(36) NOT NULL,
  `ROLE_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`CLIENT_ID`,`ROLE_ID`),
  KEY `IDX_SCOPE_MAPPING_ROLE` (`ROLE_ID`),
  CONSTRAINT `FK_OUSE064PLMLR732LXJCN1Q5F1` FOREIGN KEY (`CLIENT_ID`) REFERENCES `CLIENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SCOPE_MAPPING`
--

/*!40000 ALTER TABLE `SCOPE_MAPPING` DISABLE KEYS */;
INSERT INTO `SCOPE_MAPPING` VALUES ('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','13a15d8f-e685-4321-a8e9-9b4299dfef76'),('703aca18-8a9e-44ea-bf88-648ae965c214','25b888c8-7935-430b-8927-9d244a39060b'),('a57d63c3-4d3c-40d6-a086-9d43cd4fedd5','89603119-2285-420c-beac-aec506afd06a'),('703aca18-8a9e-44ea-bf88-648ae965c214','c8004809-83f1-4023-9a0f-0e55e797a40e');
/*!40000 ALTER TABLE `SCOPE_MAPPING` ENABLE KEYS */;

--
-- Table structure for table `SCOPE_POLICY`
--

DROP TABLE IF EXISTS `SCOPE_POLICY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SCOPE_POLICY` (
  `SCOPE_ID` varchar(36) NOT NULL,
  `POLICY_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`SCOPE_ID`,`POLICY_ID`),
  KEY `IDX_SCOPE_POLICY_POLICY` (`POLICY_ID`),
  CONSTRAINT `FK_FRSRASP13XCX4WNKOG82SSRFY` FOREIGN KEY (`POLICY_ID`) REFERENCES `RESOURCE_SERVER_POLICY` (`ID`),
  CONSTRAINT `FK_FRSRPASS3XCX4WNKOG82SSRFY` FOREIGN KEY (`SCOPE_ID`) REFERENCES `RESOURCE_SERVER_SCOPE` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SCOPE_POLICY`
--

/*!40000 ALTER TABLE `SCOPE_POLICY` DISABLE KEYS */;
/*!40000 ALTER TABLE `SCOPE_POLICY` ENABLE KEYS */;

--
-- Table structure for table `SERVER_CONFIG`
--

DROP TABLE IF EXISTS `SERVER_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SERVER_CONFIG` (
  `SERVER_CONFIG_KEY` varchar(255) NOT NULL,
  `VALUE` longtext NOT NULL,
  `VERSION` int DEFAULT '0',
  PRIMARY KEY (`SERVER_CONFIG_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SERVER_CONFIG`
--

/*!40000 ALTER TABLE `SERVER_CONFIG` DISABLE KEYS */;
/*!40000 ALTER TABLE `SERVER_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `USER_ATTRIBUTE`
--

DROP TABLE IF EXISTS `USER_ATTRIBUTE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_ATTRIBUTE` (
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `USER_ID` varchar(36) NOT NULL,
  `ID` varchar(36) NOT NULL DEFAULT 'sybase-needs-something-here',
  `LONG_VALUE_HASH` binary(64) DEFAULT NULL,
  `LONG_VALUE_HASH_LOWER_CASE` binary(64) DEFAULT NULL,
  `LONG_VALUE` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci,
  PRIMARY KEY (`ID`),
  KEY `IDX_USER_ATTRIBUTE` (`USER_ID`),
  KEY `IDX_USER_ATTRIBUTE_NAME` (`NAME`,`VALUE`),
  KEY `USER_ATTR_LONG_VALUES` (`LONG_VALUE_HASH`,`NAME`),
  KEY `USER_ATTR_LONG_VALUES_LOWER_CASE` (`LONG_VALUE_HASH_LOWER_CASE`,`NAME`),
  CONSTRAINT `FK_5HRM2VLF9QL5FU043KQEPOVBR` FOREIGN KEY (`USER_ID`) REFERENCES `USER_ENTITY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_ATTRIBUTE`
--

/*!40000 ALTER TABLE `USER_ATTRIBUTE` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_ATTRIBUTE` ENABLE KEYS */;

--
-- Table structure for table `USER_CONSENT`
--

DROP TABLE IF EXISTS `USER_CONSENT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_CONSENT` (
  `ID` varchar(36) NOT NULL,
  `CLIENT_ID` varchar(255) DEFAULT NULL,
  `USER_ID` varchar(36) NOT NULL,
  `CREATED_DATE` bigint DEFAULT NULL,
  `LAST_UPDATED_DATE` bigint DEFAULT NULL,
  `CLIENT_STORAGE_PROVIDER` varchar(36) DEFAULT NULL,
  `EXTERNAL_CLIENT_ID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_LOCAL_CONSENT` (`CLIENT_ID`,`USER_ID`),
  UNIQUE KEY `UK_EXTERNAL_CONSENT` (`CLIENT_STORAGE_PROVIDER`,`EXTERNAL_CLIENT_ID`,`USER_ID`),
  KEY `IDX_USER_CONSENT` (`USER_ID`),
  CONSTRAINT `FK_GRNTCSNT_USER` FOREIGN KEY (`USER_ID`) REFERENCES `USER_ENTITY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_CONSENT`
--

/*!40000 ALTER TABLE `USER_CONSENT` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_CONSENT` ENABLE KEYS */;

--
-- Table structure for table `USER_CONSENT_CLIENT_SCOPE`
--

DROP TABLE IF EXISTS `USER_CONSENT_CLIENT_SCOPE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_CONSENT_CLIENT_SCOPE` (
  `USER_CONSENT_ID` varchar(36) NOT NULL,
  `SCOPE_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`USER_CONSENT_ID`,`SCOPE_ID`),
  KEY `IDX_USCONSENT_CLSCOPE` (`USER_CONSENT_ID`),
  KEY `IDX_USCONSENT_SCOPE_ID` (`SCOPE_ID`),
  CONSTRAINT `FK_GRNTCSNT_CLSC_USC` FOREIGN KEY (`USER_CONSENT_ID`) REFERENCES `USER_CONSENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_CONSENT_CLIENT_SCOPE`
--

/*!40000 ALTER TABLE `USER_CONSENT_CLIENT_SCOPE` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_CONSENT_CLIENT_SCOPE` ENABLE KEYS */;

--
-- Table structure for table `USER_ENTITY`
--

DROP TABLE IF EXISTS `USER_ENTITY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_ENTITY` (
  `ID` varchar(36) NOT NULL,
  `EMAIL` varchar(255) DEFAULT NULL,
  `EMAIL_CONSTRAINT` varchar(255) DEFAULT NULL,
  `EMAIL_VERIFIED` tinyint NOT NULL DEFAULT '0',
  `ENABLED` tinyint NOT NULL DEFAULT '0',
  `FEDERATION_LINK` varchar(255) DEFAULT NULL,
  `FIRST_NAME` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `LAST_NAME` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `REALM_ID` varchar(255) DEFAULT NULL,
  `USERNAME` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `CREATED_TIMESTAMP` bigint DEFAULT NULL,
  `SERVICE_ACCOUNT_CLIENT_LINK` varchar(255) DEFAULT NULL,
  `NOT_BEFORE` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_DYKN684SL8UP1CRFEI6ECKHD7` (`REALM_ID`,`EMAIL_CONSTRAINT`),
  UNIQUE KEY `UK_RU8TT6T700S9V50BU18WS5HA6` (`REALM_ID`,`USERNAME`),
  KEY `IDX_USER_EMAIL` (`EMAIL`),
  KEY `IDX_USER_SERVICE_ACCOUNT` (`REALM_ID`,`SERVICE_ACCOUNT_CLIENT_LINK`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_ENTITY`
--

/*!40000 ALTER TABLE `USER_ENTITY` DISABLE KEYS */;
INSERT INTO `USER_ENTITY` VALUES ('2dac9aff-3c68-43d4-8f18-082789a6c867','shino2003925@gmail.com','shino2003925@gmail.com',1,1,NULL,'dev','dev','736d3e5b-4c46-41ed-9afb-47e727ecab9e','dev',1755404939710,NULL,0),('4030193c-c653-4e74-a4b4-6fd4e06fb4cf','test@example.com','test@example.com',0,1,NULL,'Nguyen','Van A','b90e8c11-9258-44ee-901a-1650b6e14601','testuser',1755975333600,NULL,0),('5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc','shino2003925@gmail.com','shino2003925@gmail.com',1,1,NULL,'dev','dev','b90e8c11-9258-44ee-901a-1650b6e14601','dev',1751819264280,NULL,0),('c60b0781-0325-4114-bf24-7fcf6ff76cbb','testuser1@example.com','testuser1@example.com',0,1,NULL,'Test','User','b90e8c11-9258-44ee-901a-1650b6e14601','testuser1',1756031808495,NULL,0),('dce7ffce-1455-4041-8f58-224b6d426684',NULL,'5dc77712-cba9-4d6a-9a00-37d916b180ea',0,1,NULL,NULL,NULL,'b90e8c11-9258-44ee-901a-1650b6e14601','service-account-doantotnghiep',1751813603969,'028d7f5b-9f03-4977-bc22-d7ddad3abbdf',0);
/*!40000 ALTER TABLE `USER_ENTITY` ENABLE KEYS */;

--
-- Table structure for table `USER_FEDERATION_CONFIG`
--

DROP TABLE IF EXISTS `USER_FEDERATION_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_FEDERATION_CONFIG` (
  `USER_FEDERATION_PROVIDER_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`USER_FEDERATION_PROVIDER_ID`,`NAME`),
  CONSTRAINT `FK_T13HPU1J94R2EBPEKR39X5EU5` FOREIGN KEY (`USER_FEDERATION_PROVIDER_ID`) REFERENCES `USER_FEDERATION_PROVIDER` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_FEDERATION_CONFIG`
--

/*!40000 ALTER TABLE `USER_FEDERATION_CONFIG` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_FEDERATION_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `USER_FEDERATION_MAPPER`
--

DROP TABLE IF EXISTS `USER_FEDERATION_MAPPER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_FEDERATION_MAPPER` (
  `ID` varchar(36) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `FEDERATION_PROVIDER_ID` varchar(36) NOT NULL,
  `FEDERATION_MAPPER_TYPE` varchar(255) NOT NULL,
  `REALM_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_USR_FED_MAP_FED_PRV` (`FEDERATION_PROVIDER_ID`),
  KEY `IDX_USR_FED_MAP_REALM` (`REALM_ID`),
  CONSTRAINT `FK_FEDMAPPERPM_FEDPRV` FOREIGN KEY (`FEDERATION_PROVIDER_ID`) REFERENCES `USER_FEDERATION_PROVIDER` (`ID`),
  CONSTRAINT `FK_FEDMAPPERPM_REALM` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_FEDERATION_MAPPER`
--

/*!40000 ALTER TABLE `USER_FEDERATION_MAPPER` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_FEDERATION_MAPPER` ENABLE KEYS */;

--
-- Table structure for table `USER_FEDERATION_MAPPER_CONFIG`
--

DROP TABLE IF EXISTS `USER_FEDERATION_MAPPER_CONFIG`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_FEDERATION_MAPPER_CONFIG` (
  `USER_FEDERATION_MAPPER_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`USER_FEDERATION_MAPPER_ID`,`NAME`),
  CONSTRAINT `FK_FEDMAPPER_CFG` FOREIGN KEY (`USER_FEDERATION_MAPPER_ID`) REFERENCES `USER_FEDERATION_MAPPER` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_FEDERATION_MAPPER_CONFIG`
--

/*!40000 ALTER TABLE `USER_FEDERATION_MAPPER_CONFIG` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_FEDERATION_MAPPER_CONFIG` ENABLE KEYS */;

--
-- Table structure for table `USER_FEDERATION_PROVIDER`
--

DROP TABLE IF EXISTS `USER_FEDERATION_PROVIDER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_FEDERATION_PROVIDER` (
  `ID` varchar(36) NOT NULL,
  `CHANGED_SYNC_PERIOD` int DEFAULT NULL,
  `DISPLAY_NAME` varchar(255) DEFAULT NULL,
  `FULL_SYNC_PERIOD` int DEFAULT NULL,
  `LAST_SYNC` int DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `PROVIDER_NAME` varchar(255) DEFAULT NULL,
  `REALM_ID` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `IDX_USR_FED_PRV_REALM` (`REALM_ID`),
  CONSTRAINT `FK_1FJ32F6PTOLW2QY60CD8N01E8` FOREIGN KEY (`REALM_ID`) REFERENCES `REALM` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_FEDERATION_PROVIDER`
--

/*!40000 ALTER TABLE `USER_FEDERATION_PROVIDER` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_FEDERATION_PROVIDER` ENABLE KEYS */;

--
-- Table structure for table `USER_GROUP_MEMBERSHIP`
--

DROP TABLE IF EXISTS `USER_GROUP_MEMBERSHIP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_GROUP_MEMBERSHIP` (
  `GROUP_ID` varchar(36) NOT NULL,
  `USER_ID` varchar(36) NOT NULL,
  `MEMBERSHIP_TYPE` varchar(255) NOT NULL,
  PRIMARY KEY (`GROUP_ID`,`USER_ID`),
  KEY `IDX_USER_GROUP_MAPPING` (`USER_ID`),
  CONSTRAINT `FK_USER_GROUP_USER` FOREIGN KEY (`USER_ID`) REFERENCES `USER_ENTITY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_GROUP_MEMBERSHIP`
--

/*!40000 ALTER TABLE `USER_GROUP_MEMBERSHIP` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_GROUP_MEMBERSHIP` ENABLE KEYS */;

--
-- Table structure for table `USER_REQUIRED_ACTION`
--

DROP TABLE IF EXISTS `USER_REQUIRED_ACTION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_REQUIRED_ACTION` (
  `USER_ID` varchar(36) NOT NULL,
  `REQUIRED_ACTION` varchar(255) NOT NULL DEFAULT ' ',
  PRIMARY KEY (`REQUIRED_ACTION`,`USER_ID`),
  KEY `IDX_USER_REQACTIONS` (`USER_ID`),
  CONSTRAINT `FK_6QJ3W1JW9CVAFHE19BWSIUVMD` FOREIGN KEY (`USER_ID`) REFERENCES `USER_ENTITY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_REQUIRED_ACTION`
--

/*!40000 ALTER TABLE `USER_REQUIRED_ACTION` DISABLE KEYS */;
/*!40000 ALTER TABLE `USER_REQUIRED_ACTION` ENABLE KEYS */;

--
-- Table structure for table `USER_ROLE_MAPPING`
--

DROP TABLE IF EXISTS `USER_ROLE_MAPPING`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `USER_ROLE_MAPPING` (
  `ROLE_ID` varchar(255) NOT NULL,
  `USER_ID` varchar(36) NOT NULL,
  PRIMARY KEY (`ROLE_ID`,`USER_ID`),
  KEY `IDX_USER_ROLE_MAPPING` (`USER_ID`),
  CONSTRAINT `FK_C4FQV34P1MBYLLOXANG7B1Q3L` FOREIGN KEY (`USER_ID`) REFERENCES `USER_ENTITY` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `USER_ROLE_MAPPING`
--

/*!40000 ALTER TABLE `USER_ROLE_MAPPING` DISABLE KEYS */;
INSERT INTO `USER_ROLE_MAPPING` VALUES ('02112d05-ab93-428e-9487-845c27a20b1f','2dac9aff-3c68-43d4-8f18-082789a6c867'),('02c57b11-11ba-4679-b9a9-a3ea45b8f157','2dac9aff-3c68-43d4-8f18-082789a6c867'),('0393b358-a958-4f83-bc13-e33b7e27a5ee','2dac9aff-3c68-43d4-8f18-082789a6c867'),('08a0b56f-7647-4340-9622-15a85b1d33d8','2dac9aff-3c68-43d4-8f18-082789a6c867'),('1c56677f-1a5f-47e7-ac29-cd386bf7765d','2dac9aff-3c68-43d4-8f18-082789a6c867'),('25180c3d-0961-4107-9462-d73128d2443f','2dac9aff-3c68-43d4-8f18-082789a6c867'),('25b888c8-7935-430b-8927-9d244a39060b','2dac9aff-3c68-43d4-8f18-082789a6c867'),('28387ce2-eece-409c-ac39-169371204360','2dac9aff-3c68-43d4-8f18-082789a6c867'),('42fc327d-f710-4e25-889b-7ebfdb1d9daa','2dac9aff-3c68-43d4-8f18-082789a6c867'),('45f31d92-6f8a-43d3-82a4-e14da34873b3','2dac9aff-3c68-43d4-8f18-082789a6c867'),('4de0e63f-abd5-427c-aab2-2ed7f8a58ec0','2dac9aff-3c68-43d4-8f18-082789a6c867'),('4eb3aa14-3436-4180-8909-aacbdf43659e','2dac9aff-3c68-43d4-8f18-082789a6c867'),('57efb658-e3b8-409b-8ae9-d406bb04335b','2dac9aff-3c68-43d4-8f18-082789a6c867'),('5bb9f798-176d-4de0-a595-06e7067df4d2','2dac9aff-3c68-43d4-8f18-082789a6c867'),('67118b40-69ce-4576-95e2-c215710a7e8c','2dac9aff-3c68-43d4-8f18-082789a6c867'),('69146af4-f2ee-40d3-a5f3-bf885dc447cc','2dac9aff-3c68-43d4-8f18-082789a6c867'),('69c0ec5c-4099-4e2d-87c9-637d35ea738e','2dac9aff-3c68-43d4-8f18-082789a6c867'),('6a28c7be-f203-4d41-8a3a-e4d9b89bd928','2dac9aff-3c68-43d4-8f18-082789a6c867'),('6b1f87ee-2c31-4de2-967e-c802c8c24b27','2dac9aff-3c68-43d4-8f18-082789a6c867'),('6bacd7b8-5e28-4ded-9332-cfa9398fd738','2dac9aff-3c68-43d4-8f18-082789a6c867'),('6be05d3b-a559-498f-bb01-daf2ca0eb51e','2dac9aff-3c68-43d4-8f18-082789a6c867'),('6ca12eef-2b56-4303-86d8-7d90befe9e87','2dac9aff-3c68-43d4-8f18-082789a6c867'),('6d242725-cf40-47db-a7a9-4be50a7d595b','2dac9aff-3c68-43d4-8f18-082789a6c867'),('7ff5492c-9bc7-43d8-87c4-b719d65547e9','2dac9aff-3c68-43d4-8f18-082789a6c867'),('82bab180-d568-4a04-a1f4-cfb93bb94599','2dac9aff-3c68-43d4-8f18-082789a6c867'),('838a91b6-a65d-4539-9d78-30a598916488','2dac9aff-3c68-43d4-8f18-082789a6c867'),('89b819f5-80eb-491c-a888-63e8dbec7c7d','2dac9aff-3c68-43d4-8f18-082789a6c867'),('8b555b9e-3761-4404-85f4-dfa581bdb5cf','2dac9aff-3c68-43d4-8f18-082789a6c867'),('8eca61e5-db73-499c-869d-063c7f52f4cc','2dac9aff-3c68-43d4-8f18-082789a6c867'),('9d2a9df4-8da5-48c3-b575-93347476f3d6','2dac9aff-3c68-43d4-8f18-082789a6c867'),('a0ffeee3-63ba-46fe-9c67-0d11938ba6e4','2dac9aff-3c68-43d4-8f18-082789a6c867'),('a2336e2e-843e-47a5-a16a-2f10216f7d44','2dac9aff-3c68-43d4-8f18-082789a6c867'),('a58950e1-dd22-42b0-952f-b2b348d48a36','2dac9aff-3c68-43d4-8f18-082789a6c867'),('a5b6690f-db3f-4772-ac95-07865102c3cb','2dac9aff-3c68-43d4-8f18-082789a6c867'),('acb63854-a657-42c4-a70d-cbd710bff9e0','2dac9aff-3c68-43d4-8f18-082789a6c867'),('ad629a28-6aa3-49a9-a10b-5cb07f3c4de3','2dac9aff-3c68-43d4-8f18-082789a6c867'),('ae248f1c-1714-431b-a1ce-3b288937f60a','2dac9aff-3c68-43d4-8f18-082789a6c867'),('bf1f74b6-8c60-4b33-a88d-e77620bae619','2dac9aff-3c68-43d4-8f18-082789a6c867'),('c5f9f487-cf44-4c73-9b16-0fde2924bd00','2dac9aff-3c68-43d4-8f18-082789a6c867'),('c70f977c-1f7d-45d3-a4b1-fecc8757df50','2dac9aff-3c68-43d4-8f18-082789a6c867'),('c8004809-83f1-4023-9a0f-0e55e797a40e','2dac9aff-3c68-43d4-8f18-082789a6c867'),('c83a4447-52e6-4451-9737-38b9619c828d','2dac9aff-3c68-43d4-8f18-082789a6c867'),('d35f090e-11ff-4e3f-ba25-7be1fd082b4c','2dac9aff-3c68-43d4-8f18-082789a6c867'),('d9245161-e9f0-4479-86e5-92d9c6ef3c83','2dac9aff-3c68-43d4-8f18-082789a6c867'),('e61e14ab-a1b6-47e3-a182-0c80ce0cc23b','2dac9aff-3c68-43d4-8f18-082789a6c867'),('e8f934e2-5ff2-414b-9ea7-7e2d53556e32','2dac9aff-3c68-43d4-8f18-082789a6c867'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','4030193c-c653-4e74-a4b4-6fd4e06fb4cf'),('80aae6a3-6bcb-4f11-80e7-1921e2035cd8','4030193c-c653-4e74-a4b4-6fd4e06fb4cf'),('071a31b5-330c-4931-b8e6-348573d4774a','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('13a15d8f-e685-4321-a8e9-9b4299dfef76','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('17a79d1e-453a-4599-bc0d-09385ba9c6ca','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('189dc51b-311a-4786-b014-74ab2bff6673','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('25e8a5c0-77d3-4ea0-a80e-2bc0b731bb92','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('368e8d6a-5b1d-46b0-9b37-2584c3accb8c','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('3a2f4883-a668-4a66-b8c0-a774c0facd86','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('3e038d42-14ca-48ee-a4bd-cec4ec558287','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('490aa571-2591-43df-b357-7f1bd4bd0c54','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('59b77fbd-731f-402e-adca-ae5c779e5505','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('696c50b7-1ca4-487a-be10-35af1b1edf30','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('75696880-7f61-45d5-b326-a5e8b021bb9f','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('7a25ff1e-92a6-4a52-9b10-1ae0b1284305','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('7cd24a2d-2b98-41da-9d40-64e04b44a0f0','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('7e1512be-5397-406b-9181-86bd319a6493','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('80aae6a3-6bcb-4f11-80e7-1921e2035cd8','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('89603119-2285-420c-beac-aec506afd06a','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('8a7a293c-d5a8-4a09-8620-9eb77007816a','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('8b453d03-a970-4732-b665-70e6266aa38e','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('9771e873-0be5-4096-aab2-6a689d4b1796','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('9964030e-8d82-4291-9dee-a23718b69590','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('b8e5dbef-a678-4071-872d-f019ec318cc6','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('bf764633-fad6-4f0b-9ded-68d89eadea28','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('d5d76baa-b456-4888-aecf-e359a4704f78','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('db9f0d3b-d226-4d74-a911-6b21e171c1d3','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('de10e7a6-8542-4240-91a9-b7323ba3c2c7','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('e5ed0422-2d74-421a-96a6-e667f85a0ebd','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('efe37c6e-668b-42b6-a496-185f95031b0f','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('f3882302-e087-474c-b21e-1efc409bc5e8','5a7b9193-de81-43c3-9fe7-c6d6b7f3a2fc'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','c60b0781-0325-4114-bf24-7fcf6ff76cbb'),('071a31b5-330c-4931-b8e6-348573d4774a','dce7ffce-1455-4041-8f58-224b6d426684'),('13a15d8f-e685-4321-a8e9-9b4299dfef76','dce7ffce-1455-4041-8f58-224b6d426684'),('17a79d1e-453a-4599-bc0d-09385ba9c6ca','dce7ffce-1455-4041-8f58-224b6d426684'),('189dc51b-311a-4786-b014-74ab2bff6673','dce7ffce-1455-4041-8f58-224b6d426684'),('25e8a5c0-77d3-4ea0-a80e-2bc0b731bb92','dce7ffce-1455-4041-8f58-224b6d426684'),('368e8d6a-5b1d-46b0-9b37-2584c3accb8c','dce7ffce-1455-4041-8f58-224b6d426684'),('3a2f4883-a668-4a66-b8c0-a774c0facd86','dce7ffce-1455-4041-8f58-224b6d426684'),('3e038d42-14ca-48ee-a4bd-cec4ec558287','dce7ffce-1455-4041-8f58-224b6d426684'),('490aa571-2591-43df-b357-7f1bd4bd0c54','dce7ffce-1455-4041-8f58-224b6d426684'),('59b77fbd-731f-402e-adca-ae5c779e5505','dce7ffce-1455-4041-8f58-224b6d426684'),('60dbc25e-b0fa-4335-8251-cef1e45d2465','dce7ffce-1455-4041-8f58-224b6d426684'),('696c50b7-1ca4-487a-be10-35af1b1edf30','dce7ffce-1455-4041-8f58-224b6d426684'),('75696880-7f61-45d5-b326-a5e8b021bb9f','dce7ffce-1455-4041-8f58-224b6d426684'),('7a25ff1e-92a6-4a52-9b10-1ae0b1284305','dce7ffce-1455-4041-8f58-224b6d426684'),('7cd24a2d-2b98-41da-9d40-64e04b44a0f0','dce7ffce-1455-4041-8f58-224b6d426684'),('7e1512be-5397-406b-9181-86bd319a6493','dce7ffce-1455-4041-8f58-224b6d426684'),('80aae6a3-6bcb-4f11-80e7-1921e2035cd8','dce7ffce-1455-4041-8f58-224b6d426684'),('89603119-2285-420c-beac-aec506afd06a','dce7ffce-1455-4041-8f58-224b6d426684'),('8a7a293c-d5a8-4a09-8620-9eb77007816a','dce7ffce-1455-4041-8f58-224b6d426684'),('8b453d03-a970-4732-b665-70e6266aa38e','dce7ffce-1455-4041-8f58-224b6d426684'),('9771e873-0be5-4096-aab2-6a689d4b1796','dce7ffce-1455-4041-8f58-224b6d426684'),('9964030e-8d82-4291-9dee-a23718b69590','dce7ffce-1455-4041-8f58-224b6d426684'),('b8e5dbef-a678-4071-872d-f019ec318cc6','dce7ffce-1455-4041-8f58-224b6d426684'),('bf764633-fad6-4f0b-9ded-68d89eadea28','dce7ffce-1455-4041-8f58-224b6d426684'),('d5d76baa-b456-4888-aecf-e359a4704f78','dce7ffce-1455-4041-8f58-224b6d426684'),('db9f0d3b-d226-4d74-a911-6b21e171c1d3','dce7ffce-1455-4041-8f58-224b6d426684'),('de10e7a6-8542-4240-91a9-b7323ba3c2c7','dce7ffce-1455-4041-8f58-224b6d426684'),('e5ed0422-2d74-421a-96a6-e667f85a0ebd','dce7ffce-1455-4041-8f58-224b6d426684'),('efe37c6e-668b-42b6-a496-185f95031b0f','dce7ffce-1455-4041-8f58-224b6d426684'),('f3882302-e087-474c-b21e-1efc409bc5e8','dce7ffce-1455-4041-8f58-224b6d426684');
/*!40000 ALTER TABLE `USER_ROLE_MAPPING` ENABLE KEYS */;

--
-- Table structure for table `WEB_ORIGINS`
--

DROP TABLE IF EXISTS `WEB_ORIGINS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `WEB_ORIGINS` (
  `CLIENT_ID` varchar(36) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  PRIMARY KEY (`CLIENT_ID`,`VALUE`),
  KEY `IDX_WEB_ORIG_CLIENT` (`CLIENT_ID`),
  CONSTRAINT `FK_LOJPHO213XCX4WNKOG82SSRFY` FOREIGN KEY (`CLIENT_ID`) REFERENCES `CLIENT` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `WEB_ORIGINS`
--

/*!40000 ALTER TABLE `WEB_ORIGINS` DISABLE KEYS */;
INSERT INTO `WEB_ORIGINS` VALUES ('028d7f5b-9f03-4977-bc22-d7ddad3abbdf','*'),('7793a774-b465-4b0b-97d1-08840aa08c9a','+'),('e12fceb4-c28d-4e75-8f26-48fe7824c110','+');
/*!40000 ALTER TABLE `WEB_ORIGINS` ENABLE KEYS */;

--
-- Dumping routines for database 'server_user'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-17 23:34:53
