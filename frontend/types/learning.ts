export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN'

export type Course = {
  id: string
  slug: string
  title: string
  subject: string
  description: string
  cover: string
  lessonsCount: number
  completedLessons: number
  progressPercent: number
}

export type CourseLesson = {
  id: string
  title: string
  minutes: number
  orderNumber: number
  completed: boolean
}

export type CourseModule = {
  id: string
  title: string
  orderNumber: number
  lessons: CourseLesson[]
}

export type CourseDetails = Course & {
  modules: CourseModule[]
}

export type LessonDetails = {
  id: string
  courseId: string
  courseSlug: string
  courseTitle: string
  moduleTitle: string
  title: string
  content: string
  videoUrl?: string
  minutes: number
  completed: boolean
}

export type LearningSummary = {
  totalLessons: number
  completedLessons: number
  progressPercent: number
}

export type TestStatus = 'DRAFT' | 'PUBLISHED'
export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE'

export type AnswerOption = {
  id: string
  optionText: string
  isCorrect: boolean
  orderNumber: number
}

export type StudentAnswerOption = Omit<AnswerOption, 'isCorrect'>

export type Question = {
  id: string
  questionText: string
  questionType: QuestionType
  explanation: string
  orderNumber: number
  options: AnswerOption[]
}

export type QuestionImportResult = {
  importedCount: number
  questions: Question[]
}

export type StudentQuestion = Omit<Question, 'explanation' | 'options'> & {
  options: StudentAnswerOption[]
}

export type Test = {
  id: string
  title: string
  slug: string
  description: string
  subject: string
  timeLimitMinutes: number
  status: TestStatus
  attempts: number
  averageScore: number
  questions: Question[]
}

export type TestSummary = Omit<Test, 'description' | 'questions'>

export type StudentTest = Omit<Test, 'status' | 'attempts' | 'averageScore' | 'questions'> & {
  questions: StudentQuestion[]
}

export type TeacherTest = Omit<Test, 'attempts' | 'averageScore'> & {
  createdBy?: string
}

export type TestAttempt = {
  id: string
  testId: string
  startedAt: string
  status: 'STARTED'
}

export type QuestionResult = {
  questionId: string
  correct: boolean
  selectedOptionIds: string[]
  correctOptionIds: string[]
  explanation: string
}

export type TestAnalytics = {
  totalQuestions: number
  answeredQuestions: number
  correctAnswers: number
  wrongAnswers: number
  skippedQuestions: number
  accuracyPercent: number
  durationSeconds: number
  durationMinutes: number
  level: string
  recommendation: string
}

export type TestResult = {
  testId: string
  testTitle: string
  attemptId: string
  score: number
  maxScore: number
  scorePercent: number
  completedAt: string
  analytics: TestAnalytics
  questions: QuestionResult[]
}

export type TeacherTestStats = {
  testId: string
  attemptsCount: number
  averageScorePercent: number
  averageTimeMinutes: number
  hardestQuestions: Array<{ questionId: string; questionText: string; wrongPercent: number }>
  students: Array<{
    attemptId: string
    studentName: string
    email: string
    score: number
    maxScore: number
    scorePercent: number
    answeredQuestions: number
    skippedQuestions: number
    durationMinutes: number
    completedAt: string
  }>
}

export type BlogPost = {
  id: string
  slug: string
  title: string
  excerpt: string
  content: string
  readMinutes: number
  category: string
  status: 'DRAFT' | 'PUBLISHED'
  authorName?: string
  likesCount: number
  commentsCount: number
  likedByMe: boolean
  createdAt: string
}

export type BlogComment = {
  id: string
  authorName: string
  authorEmail: string
  authorRole: Role
  content: string
  createdAt: string
}

export type BlogReaction = {
  likesCount: number
  likedByMe: boolean
}

export type UserProfile = {
  id: string
  fullName: string
  email: string
  roles: Role[]
}

export type UserNotification = {
  id: string
  type: string
  title: string
  message: string
  link?: string
  read: boolean
  createdAt: string
}

export type AssignmentStatus = 'ASSIGNED' | 'COMPLETED'

export type TestAssignment = {
  id: string
  testId: string
  testTitle: string
  testSlug: string
  studentName: string
  studentEmail: string
  assignedBy?: string
  status: AssignmentStatus
  assignedAt: string
  completedAt?: string
}

export type StudentAssignedTest = {
  assignmentId: string
  testId: string
  slug: string
  title: string
  subject: string
  timeLimitMinutes: number
  status: AssignmentStatus
  assignedAt: string
  completedAt?: string
}

export type ChatMessage = {
  id: string
  message: string
  senderName: string
  senderRole: Role
  mine: boolean
  createdAt: string
}
