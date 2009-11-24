-- MySQL dump 10.9
--
-- Host: localhost    Database: wikiverifier
-- ------------------------------------------------------
-- Server version	4.1.14-standard

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `diff_history`
--

DROP TABLE IF EXISTS `diff_history`;
CREATE TABLE `diff_history` (
  `id` int(8) unsigned NOT NULL auto_increment,
  `diff_id` int(8) unsigned NOT NULL default '0',
  `user_id` mediumint(8) unsigned NOT NULL default '0',
  `vdate` datetime NOT NULL default '2000-01-01 00:00:00',
  `verified` tinyint(1) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `diff_id` (`diff_id`),
  KEY `verified` (`verified`),
  KEY `user_id` (`user_id`),
  KEY `vdate` (`vdate`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `diffs`
--

DROP TABLE IF EXISTS `diffs`;
CREATE TABLE `diffs` (
  `id` int(8) unsigned NOT NULL auto_increment,
  `title` varchar(255) NOT NULL default '',
  `new_id` int(8) unsigned NOT NULL default '0',
  `old_id` int(8) unsigned NOT NULL default '0',
  `verified` tinyint(1) unsigned NOT NULL default '0',
  `successive` tinyint(1) unsigned default NULL,
  `curid` int(10) unsigned NOT NULL default '0',
  `aggregate_id` int(10) unsigned NOT NULL default '0',
  `everseen` tinyint(1) unsigned NOT NULL default '0',
  `author` varchar(255) NOT NULL default '',
  `addedon` datetime NOT NULL default '2000-01-01 00:00:00',
  `modifiedon_utc` datetime NOT NULL default '2000-01-01 00:00:00',
  PRIMARY KEY  (`id`),
  KEY `title` (`title`),
  KEY `new_id` (`new_id`),
  KEY `old_id` (`old_id`),
  KEY `verified` (`verified`),
  KEY `aggregate_id` (`aggregate_id`),
  KEY `author` (`author`),
  KEY `curid` (`curid`),
  KEY `addedon` (`addedon`),
  KEY `modifiedon` (`modifiedon_utc`),
  KEY `successive` (`successive`),
  KEY `successive_2` (`successive`,`verified`),
  KEY `successive_3` (`successive`,`verified`,`modifiedon_utc`),
  KEY `successive_4` (`successive`,`verified`,`new_id`),
  KEY `verified_2` (`verified`,`everseen`,`curid`,`modifiedon_utc`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `familiars`
--

DROP TABLE IF EXISTS `familiars`;
CREATE TABLE `familiars` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `uname` varchar(255) NOT NULL default 'no username',
  `disabled` tinyint(1) unsigned NOT NULL default '0',
  `addedby` mediumint(8) unsigned NOT NULL default '0',
  `addedon` datetime NOT NULL default '2000-01-01 00:00:00',
  PRIMARY KEY  (`id`),
  KEY `uname` (`uname`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `hits`
--

DROP TABLE IF EXISTS `hits`;
CREATE TABLE `hits` (
  `id` int(8) unsigned NOT NULL auto_increment,
  `hdate` datetime NOT NULL default '2000-01-01 00:00:00',
  `htype` varchar(255) NOT NULL default '',
  `user_id` mediumint(8) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `hdate` (`hdate`),
  KEY `htype` (`htype`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `people`
--

DROP TABLE IF EXISTS `people`;
CREATE TABLE `people` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `uname` varchar(255) NOT NULL default 'no username',
  `pwd` varchar(32) NOT NULL default 'no password',
  `last_login` datetime NOT NULL default '2000-01-01 00:00:00',
  `prev_login` datetime NOT NULL default '2000-01-01 00:00:00',
  `changedpwd` tinyint(1) unsigned NOT NULL default '0',
  `rights` varchar(50) NOT NULL default '',
  `disabled` tinyint(1) unsigned NOT NULL default '0',
  `addedby` mediumint(8) unsigned NOT NULL default '0',
  `addedon` datetime NOT NULL default '2000-01-01 00:00:00',
  PRIMARY KEY  (`id`),
  KEY `uname` (`uname`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `robots`
--

DROP TABLE IF EXISTS `robots`;
CREATE TABLE `robots` (
  `id` mediumint(8) unsigned NOT NULL auto_increment,
  `uname` varchar(255) NOT NULL default 'no username',
  `disabled` tinyint(1) unsigned NOT NULL default '0',
  `addedon` datetime NOT NULL default '2000-01-01 00:00:00',
  PRIMARY KEY  (`id`),
  KEY `uname` (`uname`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

