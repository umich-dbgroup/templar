CREATE TABLE IF NOT EXISTS Posts (
    Id INTEGER PRIMARY KEY,
    PostTypeId INTEGER,
    AcceptedAnswerId INTEGER,
    ParentId INTEGER,
    CreationDate DATETIME,
    DeletionDate DATETIME,
    Score INTEGER,
    ViewCount INTEGER,
    Body TEXT,
    OwnerUserId INTEGER,
    OwnerDisplayName VARCHAR(1000),
    LastEditorUserId INTEGER,
    LastEditorDisplayName VARCHAR(1000),
    LastEditDate DATETIME,
    LastActivityDate DATETIME,
    Title VARCHAR(1000),
    Tags VARCHAR(1000),
    AnswerCount INTEGER,
    CommentCount INTEGER,
    FavoriteCount INTEGER,
    ClosedDate DATETIME,
    CommunityOwnedDate DATETIME
);

CREATE TABLE IF NOT EXISTS PostsWithDeleted (
    Id INTEGER PRIMARY KEY,
    PostTypeId INTEGER,
    AcceptedAnswerId INTEGER,
    ParentId INTEGER,
    CreationDate DATETIME,
    DeletionDate DATETIME,
    Score INTEGER,
    ViewCount INTEGER,
    Body TEXT,
    OwnerUserId INTEGER,
    OwnerDisplayName VARCHAR(1000),
    LastEditorUserId INTEGER,
    LastEditorDisplayName VARCHAR(1000),
    LastEditDate DATETIME,
    LastActivityDate DATETIME,
    Title VARCHAR(1000),
    Tags VARCHAR(1000),
    AnswerCount INTEGER,
    CommentCount INTEGER,
    FavoriteCount INTEGER,
    ClosedDate DATETIME,
    CommunityOwnedDate DATETIME
);

CREATE TABLE IF NOT EXISTS Users (
    Id INTEGER PRIMARY KEY,
    Reputation INTEGER,
    CreationDate DATETIME,
    DisplayName VARCHAR(1000),
    LastAccessDate DATETIME,
    WebsiteUrl VARCHAR(1000),
    Location VARCHAR(1000),
    AboutMe TEXT,
    Views INTEGER,
    UpVotes INTEGER,
    DownVotes INTEGER,
    ProfileImageUrl VARCHAR(1000),
    EmailHash VARCHAR(1000),
    AccountId INTEGER,
    Age INTEGER
);

CREATE TABLE IF NOT EXISTS Comments (
    Id INTEGER PRIMARY KEY,
    UserId INTEGER,
    PostId INTEGER,
    Score INTEGER,
    Text TEXT,
    CreationDate DATETIME,
    UserDisplayName VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS Badges (
    Id INTEGER PRIMARY KEY,
    UserId INTEGER,
    Name VARCHAR(1000),
    Date DATETIME,
    Class INTEGER,
    TagBased INTEGER
);

CREATE TABLE IF NOT EXISTS CloseAsOffTopicReasonTypes (
    Id INTEGER PRIMARY KEY,
    IsUniversal INTEGER,
    MarkdownMini TEXT,
    CreationDate DATETIME,
    CreationModeratorId INTEGER,
    ApprovalDate DATETIME,
    ApprovalModeratorId INTEGER,
    DeactivationDate DATETIME,
    DeactivationModeratorId INTEGER
);

CREATE TABLE IF NOT EXISTS PendingFlags (
    Id INTEGER PRIMARY KEY,
    FlagTypeId INTEGER,
    PostId INTEGER,
    CreationDate DATETIME,
    CloseReasonTypeId INTEGER,
    CloseAsOffTopicReasonTypeId INTEGER,
    DuplicateOfQuestionId INTEGER,
    BelongsOnBaseHostAddress VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS PostFeedback (
    Id INTEGER PRIMARY KEY,
    PostId INTEGER,
    IsAnonymous INTEGER,
    VoteTypeId INTEGER,
    CreationDate DATETIME
);

CREATE TABLE IF NOT EXISTS PostHistory (
    Id INTEGER PRIMARY KEY,
    PostHistoryTypeId INTEGER,
    PostId INTEGER,
    UserId INTEGER,
    UserDisplayName VARCHAR(1000),
    Text TEXT,
    RevisionGUID INTEGER,
    CreationDate DATETIME,
    IsAnonymous INTEGER,
    VoteTypeId INTEGER,
    Comment TEXT
);

CREATE TABLE IF NOT EXISTS PostLinks (
    Id INTEGER PRIMARY KEY,
    PostId INTEGER,
    RelatedPostId INTEGER,
    LinkTypeId INTEGER,
    CreationDate DATETIME
);

CREATE TABLE IF NOT EXISTS PostNotices (
    Id INTEGER PRIMARY KEY,
    PostId INTEGER,
    PostNoticeTypeId INTEGER,
    CreationDate DATETIME,
    DeletionDate DATETIME,
    ExpiryDate DATETIME,
    Body TEXT,
    OwnerUserId INTEGER,
    DeletionUserId INTEGER
);

CREATE TABLE IF NOT EXISTS PostNoticeTypes (
    Id INTEGER PRIMARY KEY,
    ClassId INTEGER,
    Name VARCHAR(1000),
    Body TEXT,
    IsHidden INTEGER,
    Predefined INTEGER,
    PostNoticeDurationId INTEGER
);

CREATE TABLE IF NOT EXISTS PostTags (
    PostId INTEGER,
    TagId INTEGER
);

CREATE TABLE IF NOT EXISTS ReviewRejectionReasons (
    Id INTEGER,
    Name VARCHAR(1000),
    Description TEXT,
    PostTypeId INTEGER
);

CREATE TABLE IF NOT EXISTS ReviewTaskResults (
    Id INTEGER,
    ReviewTaskId INTEGER,
    ReviewTaskResultTypeId INTEGER,
    CreationDate DATETIME,
    RejectionReasonId INTEGER,
    Comment TEXT
);

CREATE TABLE IF NOT EXISTS ReviewTasks (
    Id INTEGER,
    ReviewTaskTypeId INTEGER,
    CreationDate DATETIME,
    DeletionDate DATETIME,
    ReviewTaskStateId INTEGER,
    PostId INTEGER,
    SuggestedEditId INTEGER,
    CompletedByReviewTaskId INTEGER
);

CREATE TABLE IF NOT EXISTS SuggestedEdits (
    Id INTEGER,
    PostId INTEGER,
    CreationDate DATETIME,
    ApprovalDate DATETIME,
    RejectionDate DATETIME,
    OwnerUserId INTEGER,
    Comment TEXT,
    Text TEXT,
    Title VARCHAR(1000),
    Tags VARCHAR(1000),
    RevisionGUID INTEGER
);

CREATE TABLE IF NOT EXISTS SuggestedEditVotes (
    Id INTEGER,
    SuggestedEditId INTEGER,
    UserId INTEGER,
    VoteTypeId INTEGER,
    CreationDate DATETIME,
    TargetUserId INTEGER,
    TargetRepChange INTEGER
);

CREATE TABLE IF NOT EXISTS Tags (
    Id INTEGER,
    TagName VARCHAR(1000),
    Count INTEGER,
    ExcerptPostId INTEGER,
    WikiPostId INTEGER
);

CREATE TABLE IF NOT EXISTS TagSynonyms (
    Id INTEGER,
    SourceTagName VARCHAR(1000),
    TargetTagName VARCHAR(1000),
    CreationDate DATETIME,
    OwnerUserId INTEGER,
    AutoRenameCount INTEGER,
    LastAutoRename DATETIME,
    Score INTEGER,
    ApprovedByUserId INTEGER,
    ApprovalDate DATETIME
);

CREATE TABLE IF NOT EXISTS Votes (
    Id INTEGER,
    PostId INTEGER,
    VoteTypeId INTEGER,
    UserId INTEGER,
    CreationDate DATETIME,
    BountyAmount INTEGER
);

CREATE TABLE IF NOT EXISTS PostTypes (
    Id INTEGER,
    Name VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS CloseReasonTypes (
    Id INTEGER,
    Name VARCHAR(1000),
    Description TEXT
);

CREATE TABLE IF NOT EXISTS FlagTypes (
    Id INTEGER,
    Name VARCHAR(1000),
    Description TEXT
);

CREATE TABLE IF NOT EXISTS PostTypes (
    Id INTEGER,
    Name VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS PostHistoryTypes (
    Id INTEGER,
    Name VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS VoteTypes (
    Id INTEGER,
    Name VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS ReviewTaskResultTypes (
    Id INTEGER,
    Name VARCHAR(1000),
    Description TEXT
);

CREATE TABLE IF NOT EXISTS ReviewTaskTypes (
    Id INTEGER,
    Name VARCHAR(1000),
    Description TEXT
);

CREATE TABLE IF NOT EXISTS ReviewTaskStates (
    Id INTEGER,
    Name VARCHAR(1000),
    Description TEXT
);

-- create and populate size table
DROP TABLE IF EXISTS size;
CREATE TABLE size(
    size INTEGER,
    relation VARCHAR(255)
);

INSERT INTO size SELECT COUNT(*), "Posts" FROM Posts;
INSERT INTO size SELECT COUNT(*), "PostsWithDeleted" FROM PostsWithDeleted;
INSERT INTO size SELECT COUNT(*), "Users" FROM Users;
INSERT INTO size SELECT COUNT(*), "Comments" FROM Comments;
INSERT INTO size SELECT COUNT(*), "Badges" FROM Badges;
INSERT INTO size SELECT COUNT(*), "CloseAsOffTopicReasonTypes" FROM CloseAsOffTopicReasonTypes;
INSERT INTO size SELECT COUNT(*), "PendingFlags" FROM PendingFlags;
INSERT INTO size SELECT COUNT(*), "PostFeedback" FROM PostFeedback;
INSERT INTO size SELECT COUNT(*), "PostHistory" FROM PostHistory;
INSERT INTO size SELECT COUNT(*), "PostLinks" FROM PostLinks;
INSERT INTO size SELECT COUNT(*), "PostNotices" FROM PostNotices;
INSERT INTO size SELECT COUNT(*), "PostNoticeTypes" FROM PostNoticeTypes;
INSERT INTO size SELECT COUNT(*), "PostTags" FROM PostTags;
INSERT INTO size SELECT COUNT(*), "ReviewRejectionReasons" FROM ReviewRejectionReasons;
INSERT INTO size SELECT COUNT(*), "ReviewTaskResults" FROM ReviewTaskResults;
INSERT INTO size SELECT COUNT(*), "ReviewTasks" FROM ReviewTasks;
INSERT INTO size SELECT COUNT(*), "SuggestedEdits" FROM SuggestedEdits;
INSERT INTO size SELECT COUNT(*), "SuggestedEditVotes" FROM SuggestedEditVotes;
INSERT INTO size SELECT COUNT(*), "Tags" FROM Tags;
INSERT INTO size SELECT COUNT(*), "TagSynonyms" FROM TagSynonyms;
INSERT INTO size SELECT COUNT(*), "Votes" FROM Votes;
INSERT INTO size SELECT COUNT(*), "PostTypes" FROM PostTypes;
INSERT INTO size SELECT COUNT(*), "CloseReasonTypes" FROM CloseReasonTypes;
INSERT INTO size SELECT COUNT(*), "FlagTypes" FROM FlagTypes;
INSERT INTO size SELECT COUNT(*), "PostTypes" FROM PostTypes;
INSERT INTO size SELECT COUNT(*), "PostHistoryTypes" FROM PostHistoryTypes;
INSERT INTO size SELECT COUNT(*), "VoteTypes" FROM VoteTypes;
INSERT INTO size SELECT COUNT(*), "ReviewTaskResultTypes" FROM ReviewTaskResultTypes;
INSERT INTO size SELECT COUNT(*), "ReviewTaskTypes" FROM ReviewTaskTypes;
INSERT INTO size SELECT COUNT(*), "ReviewTaskStates" FROM ReviewTaskStates;

-- Load data from sample database
-- LOAD XML LOCAL INFILE 'db_sample/Badges_100.xml' INTO TABLE Badges;
-- LOAD XML LOCAL INFILE 'db_sample/Comments_100.xml' INTO TABLE Comments;
-- LOAD XML LOCAL INFILE 'db_sample/PostHistory_100.xml' INTO TABLE PostHistory;
-- LOAD XML LOCAL INFILE 'db_sample/PostLinks_100.xml' INTO TABLE PostLinks;
-- LOAD XML LOCAL INFILE 'db_sample/Posts_100.xml' INTO TABLE Posts;
-- LOAD XML LOCAL INFILE 'db_sample/Tags_100.xml' INTO TABLE Tags;
-- LOAD XML LOCAL INFILE 'db_sample/Users_100.xml' INTO TABLE Users;
-- LOAD XML LOCAL INFILE 'db_sample/Votes_100.xml' INTO TABLE Votes;
